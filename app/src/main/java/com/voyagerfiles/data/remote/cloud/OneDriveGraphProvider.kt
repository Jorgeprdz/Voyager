package com.voyagerfiles.data.remote.cloud

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** IDs are opaque Graph identifiers, never local paths or URLs supplied by an untrusted file. */
object OneDriveGraphPaths {
    const val BASE = "https://graph.microsoft.com/v1.0"
    fun component(value: String): String {
        require(value.isNotBlank() && value.length <= 1024 && value.none { it.isISOControl() })
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }
    fun item(id: String): String =
        if (id == "root") "$BASE/me/drive/root" else "$BASE/me/drive/items/${component(id)}"
    fun children(parentId: String, pageSize: Int): String {
        require(pageSize in 1..200)
        return "${item(parentId)}/children?%24top=$pageSize"
    }
    fun search(query: String, pageSize: Int): String {
        require(query.isNotBlank() && pageSize in 1..200)
        val escaped = query.replace("'", "''")
        return "$BASE/me/drive/root/search(q='${component(escaped)}')?%24top=$pageSize"
    }
    fun upload(parentId: String, name: String): String =
        "${item(parentId)}:/${component(name)}:/content"

    /** A continuation must remain on the same official Graph API; never follow arbitrary URLs. */
    fun page(token: String): String {
        val uri = URI(token)
        require(uri.scheme == "https" && uri.host == "graph.microsoft.com" &&
            uri.port == -1 && uri.userInfo == null && uri.fragment == null &&
            uri.rawPath.startsWith("/v1.0/me/drive/")) { "Invalid OneDrive pagination URL" }
        return token
    }
}

fun interface OneDriveTokenProvider {
    /** Tokens come only from an authorized public-client OAuth flow, never from source code. */
    suspend fun token(): String?
}

data class OneDriveHttpResult(val status: Int, val body: String, val location: String? = null)

interface OneDriveHttp {
    suspend fun request(
        method: String, url: String, bearer: String?,
        body: ByteArray? = null, mimeType: String? = null,
    ): OneDriveHttpResult

    suspend fun stream(
        url: String, bearer: String?, target: OutputStream,
        cancel: CloudCancellation, progress: (Long) -> Unit,
    ): OneDriveHttpResult
}

/** Network adapter with bounded response bodies and NO bearer on content redirects. */
class OneDriveOkHttp(
    private val client: OkHttpClient = OkHttpClient.Builder().followRedirects(false).build(),
) : OneDriveHttp {
    override suspend fun request(
        method: String, url: String, bearer: String?, body: ByteArray?, mimeType: String?,
    ): OneDriveHttpResult = withContext(Dispatchers.IO) {
        require(URI(url).scheme == "https")
        val payload = body?.toRequestBody((mimeType ?: "application/json").toMediaTypeOrNull())
        val req = Request.Builder().url(url).apply {
            bearer?.let { header("Authorization", "Bearer $it") }
            header("Accept", "application/json")
        }.method(method, payload).build()
        client.newCall(req).execute().use { response ->
            val bytes = response.body?.byteStream()?.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    if (output.size() + n > 1_048_576) throw IOException("Graph response exceeds limit")
                    output.write(buffer, 0, n)
                }
                output.toString(Charsets.UTF_8.name())
            }.orEmpty()
            OneDriveHttpResult(response.code, bytes, response.header("Location"))
        }
    }

    override suspend fun stream(
        url: String, bearer: String?, target: OutputStream,
        cancel: CloudCancellation, progress: (Long) -> Unit,
    ): OneDriveHttpResult = withContext(Dispatchers.IO) {
        var currentUrl = url
        var token: String? = bearer
        repeat(5) { redirect ->
            val uri = URI(currentUrl)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() &&
                uri.userInfo == null && uri.port == -1) { "Unsafe download redirect" }
            val req = Request.Builder().url(currentUrl).apply {
                token?.let { header("Authorization", "Bearer $it") }
            }.get().build()
            client.newCall(req).execute().use { response ->
                if (response.code in 300..399) {
                    val next = response.header("Location") ?: throw IOException("Missing redirect URL")
                    val dest = URI(next)
                    require(dest.scheme == "https" && !dest.host.isNullOrBlank() &&
                        dest.port == -1 && dest.userInfo == null) { "Unsafe download redirect" }
                    currentUrl = next
                    // Preauthorized OneDrive URL MUST NOT receive the Graph access token.
                    token = if (dest.host == "graph.microsoft.com") bearer else null
                } else {
                    if (!response.isSuccessful) return@withContext OneDriveHttpResult(response.code, "")
                    var total = 0L
                    response.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            if (cancel.isCancelled()) throw IOException("Transfer cancelled")
                            val n = input.read(buffer)
                            if (n < 0) break
                            target.write(buffer, 0, n)
                            total += n
                            progress(total)
                        }
                    } ?: throw IOException("Missing download body")
                    return@withContext OneDriveHttpResult(response.code, "")
                }
            }
        }
        throw IOException("Too many OneDrive redirects")
    }
}

class OneDriveGraphException(val status: Int, val classification: CloudFailure) :
    IOException("OneDrive operation failed (HTTP $status, $classification)")

/**
 * Actual Graph request adapter. A public-client MSAL token provider must be configured
 * before this class is ever instantiated by the app; no mock account is shown as connected.
 */
class OneDriveGraphProvider(
    private val tokenProvider: OneDriveTokenProvider,
    private val http: OneDriveHttp = OneDriveOkHttp(),
) : CloudFileProvider {
    override val service: CloudService = CloudService.ONEDRIVE
    override val capabilities: Set<CloudCapability> = setOf(
        CloudCapability.LIST, CloudCapability.SEARCH, CloudCapability.DOWNLOAD,
        CloudCapability.UPLOAD, CloudCapability.CREATE, CloudCapability.RENAME,
        CloudCapability.MOVE, CloudCapability.TRASH,
    )
    @Volatile private var state = CloudConnectionState.AUTHORIZATION_REQUIRED
    @Volatile private var disconnected = false
    override val connectionState: CloudConnectionState get() = state

    private suspend fun accessToken(): String {
        if (disconnected) throw IOException("OneDrive disconnected")
        val token = tokenProvider.token()
        if (token.isNullOrBlank()) {
            state = CloudConnectionState.AUTHORIZATION_REQUIRED
            throw IOException("OneDrive authorization required")
        }
        return token
    }

    private fun validate(reply: OneDriveHttpResult): OneDriveHttpResult {
        if (reply.status !in 200..299) {
            val failure = CloudFailurePolicy.fromHttp(reply.status)
            state = when (failure) {
                CloudFailure.AUTH_EXPIRED -> CloudConnectionState.EXPIRED
                CloudFailure.SERVER_UNAVAILABLE, CloudFailure.RATE_LIMITED -> CloudConnectionState.OFFLINE
                else -> state
            }
            throw OneDriveGraphException(reply.status, failure)
        }
        state = CloudConnectionState.ONLINE
        return reply
    }

    private suspend fun json(method: String, url: String, body: JSONObject? = null): JSONObject {
        val reply = validate(http.request(
            method, url, accessToken(),
            body?.toString()?.toByteArray(Charsets.UTF_8),
            if (body != null) "application/json" else null,
        ))
        return if (reply.body.isBlank()) JSONObject() else JSONObject(reply.body)
    }

    private fun parse(item: JSONObject): CloudEntry {
        val folder = item.optJSONObject("folder") != null
        return CloudEntry(
            id = item.getString("id"),
            parentId = item.optJSONObject("parentReference")?.optString("id"),
            name = item.getString("name"),
            mimeType = if (folder) "inode/directory"
                else item.optJSONObject("file")?.optString("mimeType")
                    ?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
            isFolder = folder,
            sizeBytes = if (item.has("size") && !item.isNull("size")) item.optLong("size") else null,
            modifiedAt = item.optString("lastModifiedDateTime").takeIf { it.isNotBlank() },
        )
    }

    private suspend fun page(url: String): CloudPage {
        val json = json("GET", url)
        val values = json.getJSONArray("value")
        val entries = (0 until values.length()).map { parse(values.getJSONObject(it)) }
        val next = json.optString("@odata.nextLink").takeIf { it.isNotBlank() }?.let(OneDriveGraphPaths::page)
        return CloudPage(entries, next)
    }

    override suspend fun list(parentId: String, pageToken: String?, pageSize: Int) = runCatching {
        page(if (pageToken == null) OneDriveGraphPaths.children(parentId, pageSize)
            else OneDriveGraphPaths.page(pageToken))
    }

    override suspend fun search(text: String, pageToken: String?, pageSize: Int) = runCatching {
        page(if (pageToken == null) OneDriveGraphPaths.search(text, pageSize)
            else OneDriveGraphPaths.page(pageToken))
    }

    override suspend fun download(
        remoteId: String, target: OutputStream, cancel: CloudCancellation,
        progress: (Long) -> Unit,
    ) = runCatching {
        validate(http.stream("${OneDriveGraphPaths.item(remoteId)}/content",
            accessToken(), target, cancel, progress))
        Unit
    }

    override suspend fun upload(
        parentId: String, name: String, mimeType: String, source: InputStream,
        length: Long?, cancel: CloudCancellation, progress: (Long) -> Unit,
    ) = runCatching {
        require(length != null && length in 0..4_194_304L) {
            "Resumable large-file upload is not yet implemented"
        }
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(32768)
        while (true) {
            if (cancel.isCancelled()) throw IOException("Upload cancelled")
            val read = source.read(buffer)
            if (read < 0) break
            require(bytes.size() + read <= 4_194_304) { "Upload exceeds small-file limit" }
            bytes.write(buffer, 0, read)
            progress(bytes.size().toLong())
        }
        require(bytes.size().toLong() == length) { "Source length changed during upload" }
        val reply = validate(http.request("PUT", OneDriveGraphPaths.upload(parentId, name),
            accessToken(), bytes.toByteArray(), mimeType))
        parse(JSONObject(reply.body))
    }

    override suspend fun createFolder(parentId: String, name: String) = runCatching {
        val payload = JSONObject().put("name", name)
            .put("folder", JSONObject()).put("@microsoft.graph.conflictBehavior", "fail")
        parse(json("POST", OneDriveGraphPaths.children(parentId, 100).substringBefore("?"), payload))
    }
    override suspend fun rename(remoteId: String, newName: String) = runCatching {
        parse(json("PATCH", OneDriveGraphPaths.item(remoteId), JSONObject().put("name", newName)))
    }
    override suspend fun move(remoteId: String, newParentId: String) = runCatching {
        parse(json("PATCH", OneDriveGraphPaths.item(remoteId),
            JSONObject().put("parentReference", JSONObject().put("id", newParentId))))
    }

    /** Graph copy is asynchronous (202 + monitor URL). Never return false completion. */
    override suspend fun copy(remoteId: String, newParentId: String): Result<CloudEntry> =
        Result.failure(UnsupportedOperationException("Graph asynchronous copy not implemented"))

    /** Graph DELETE on driveItem moves it to its recycle bin when supported by the drive. */
    override suspend fun trash(remoteId: String) = runCatching {
        json("DELETE", OneDriveGraphPaths.item(remoteId))
        Unit
    }

    override suspend fun disconnect() {
        disconnected = true
        state = CloudConnectionState.DISCONNECTED
    }
}
