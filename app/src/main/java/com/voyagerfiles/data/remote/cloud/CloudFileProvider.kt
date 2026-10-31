package com.voyagerfiles.data.remote.cloud

import java.io.InputStream
import java.io.OutputStream

/**
 * Remote identifiers are opaque. Never convert them to java.io.File or assume POSIX paths.
 * A session must be explicitly authorized by the user before an implementation can connect.
 */
enum class CloudService { GOOGLE_DRIVE, ONEDRIVE }
enum class CloudCapability { LIST, SEARCH, DOWNLOAD, UPLOAD, CREATE, RENAME, MOVE, COPY, TRASH }
enum class CloudConnectionState { DISCONNECTED, AUTHORIZATION_REQUIRED, CONNECTING, ONLINE, OFFLINE, EXPIRED }

data class CloudEntry(
    val id: String,
    val parentId: String?,
    val name: String,
    val mimeType: String,
    val isFolder: Boolean,
    val sizeBytes: Long? = null,
    val modifiedAt: String? = null,
    val isNativeDocument: Boolean = false,
)

data class CloudPage(
    val entries: List<CloudEntry>,
    val nextToken: String? = null,
)

fun interface CloudCancellation {
    fun isCancelled(): Boolean
}

interface CloudFileProvider {
    val service: CloudService
    val capabilities: Set<CloudCapability>
    val connectionState: CloudConnectionState

    /** Folder and page tokens are provider-owned opaque identifiers. */
    suspend fun list(parentId: String, pageToken: String? = null, pageSize: Int = 100): Result<CloudPage>
    suspend fun search(text: String, pageToken: String? = null, pageSize: Int = 100): Result<CloudPage>
    suspend fun download(
        remoteId: String,
        target: OutputStream,
        cancel: CloudCancellation = CloudCancellation { false },
        progress: (Long) -> Unit = {},
    ): Result<Unit>
    suspend fun upload(
        parentId: String,
        name: String,
        mimeType: String,
        source: InputStream,
        length: Long?,
        cancel: CloudCancellation = CloudCancellation { false },
        progress: (Long) -> Unit = {},
    ): Result<CloudEntry>
    suspend fun createFolder(parentId: String, name: String): Result<CloudEntry>
    suspend fun rename(remoteId: String, newName: String): Result<CloudEntry>
    suspend fun move(remoteId: String, newParentId: String): Result<CloudEntry>
    suspend fun copy(remoteId: String, newParentId: String): Result<CloudEntry>
    /** Trash is not equivalent to permanent deletion. */
    suspend fun trash(remoteId: String): Result<Unit>
    suspend fun disconnect()
}

enum class CloudFailure {
    AUTH_EXPIRED, ACCESS_DENIED, NOT_FOUND, CONFLICT, RATE_LIMITED,
    SERVER_UNAVAILABLE, BAD_REQUEST, NETWORK, CANCELLED, UNKNOWN
}

object CloudFailurePolicy {
    fun fromHttp(status: Int): CloudFailure = when (status) {
        400 -> CloudFailure.BAD_REQUEST
        401 -> CloudFailure.AUTH_EXPIRED
        403 -> CloudFailure.ACCESS_DENIED
        404, 410 -> CloudFailure.NOT_FOUND
        409, 412 -> CloudFailure.CONFLICT
        429 -> CloudFailure.RATE_LIMITED
        in 500..599 -> CloudFailure.SERVER_UNAVAILABLE
        else -> CloudFailure.UNKNOWN
    }

    fun canRetry(failure: CloudFailure, attempt: Int): Boolean =
        attempt in 0..3 && failure in setOf(
            CloudFailure.RATE_LIMITED, CloudFailure.SERVER_UNAVAILABLE, CloudFailure.NETWORK
        )

    /** Exponential backoff is bounded; a server Retry-After header takes precedence. */
    fun retryDelayMillis(attempt: Int, retryAfterSeconds: Long? = null): Long {
        retryAfterSeconds?.takeIf { it >= 0 }?.let { return (it.coerceAtMost(120) * 1000) }
        return (1000L shl attempt.coerceIn(0, 6)).coerceAtMost(60000L)
    }
}

/** Collect a folder safely: never loop forever on a repeated/invalid page token. */
object CloudPageCollector {
    suspend fun collect(maxPages: Int = 100, fetch: suspend (String?) -> CloudPage): List<CloudEntry> {
        require(maxPages in 1..1000)
        val seenTokens = mutableSetOf<String>()
        val result = mutableListOf<CloudEntry>()
        var token: String? = null
        repeat(maxPages) {
            val page = fetch(token)
            result.addAll(page.entries)
            val next = page.nextToken?.takeIf { it.isNotBlank() } ?: return result
            check(seenTokens.add(next)) { "Cloud listing repeated a page token" }
            token = next
        }
        throw IllegalStateException("Cloud listing exceeded page limit")
    }
}

/** Google Workspace files need explicit export; never download them as raw binary media. */
object CloudDocumentPolicy {
    private const val prefix = "application/vnd.google-apps."
    fun requiresExport(mimeType: String): Boolean = mimeType.startsWith(prefix) &&
        mimeType != "application/vnd.google-apps.folder"

    fun exportMime(mimeType: String): String? = when (mimeType) {
        "application/vnd.google-apps.document" -> "application/pdf"
        "application/vnd.google-apps.spreadsheet" -> "text/csv"
        "application/vnd.google-apps.presentation" -> "application/pdf"
        else -> null
    }
}
