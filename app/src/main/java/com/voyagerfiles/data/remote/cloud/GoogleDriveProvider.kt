package com.voyagerfiles.data.remote.cloud

import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject

/** Drive v3, opaque IDs and server pagination. Uses the shared bounded HTTPS transport. */
class GoogleDriveProvider(
    private val tokens: OneDriveTokenProvider,
    private val http: OneDriveHttp = OneDriveOkHttp(),
) : CloudFileProvider {
    override val service = CloudService.GOOGLE_DRIVE
    override val capabilities = CloudCapability.entries.toSet()
    private var disconnected = false
    override var connectionState = CloudConnectionState.AUTHORIZATION_REQUIRED
        private set
    private val base = "https://www.googleapis.com/drive/v3/files"
    private val fields = "id,name,mimeType,size,modifiedTime,parents"
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    private fun item(id: String) = "$base/${enc(id)}"
    private suspend fun token(): String {
        check(!disconnected) { "Disconnected" }
        return tokens.token()?.takeIf { it.isNotBlank() } ?: error("Authorization required")
    }
    private fun validate(result: OneDriveHttpResult): OneDriveHttpResult {
        if (result.status !in 200..299) {
            connectionState = if (result.status == 401) CloudConnectionState.EXPIRED else CloudConnectionState.OFFLINE
            throw OneDriveGraphException(result.status, CloudFailurePolicy.fromHttp(result.status))
        }
        connectionState = CloudConnectionState.ONLINE
        return result
    }
    private suspend fun json(method: String, url: String, body: JSONObject? = null): JSONObject {
        val result = validate(http.request(method, url, token(), body?.toString()?.toByteArray(), "application/json"))
        return if (result.body.isBlank()) JSONObject() else JSONObject(result.body)
    }
    private fun parse(o: JSONObject): CloudEntry {
        val mime = o.getString("mimeType")
        return CloudEntry(o.getString("id"), o.optJSONArray("parents")?.optString(0),
            o.getString("name"), mime, mime == "application/vnd.google-apps.folder",
            o.optString("size").toLongOrNull(), o.optString("modifiedTime"), CloudDocumentPolicy.requiresExport(mime))
    }
    private suspend fun page(query: String, pageToken: String?, pageSize: Int): CloudPage {
        require(pageSize in 1..200)
        val reply = json("GET", "$base?q=${enc("trashed = false and ($query)")}&pageSize=$pageSize&fields=${enc("nextPageToken,files($fields)")}" +
            (pageToken?.let { "&pageToken=${enc(it)}" } ?: ""))
        val entries = reply.getJSONArray("files")
        return CloudPage((0 until entries.length()).map { parse(entries.getJSONObject(it)) },
            reply.optString("nextPageToken").takeIf { it.isNotBlank() })
    }
    private fun quoted(value: String) = value.replace("\\", "\\\\").replace("'", "\\'")
    override suspend fun list(parentId: String, pageToken: String?, pageSize: Int) = runCatching {
        page("'${quoted(parentId)}' in parents", pageToken, pageSize)
    }
    override suspend fun search(text: String, pageToken: String?, pageSize: Int) = runCatching {
        require(text.isNotBlank()); page("name contains '${quoted(text)}'", pageToken, pageSize)
    }
    override suspend fun download(remoteId: String, target: OutputStream, cancel: CloudCancellation, progress: (Long) -> Unit) = runCatching {
        val metadata = parse(json("GET", "${item(remoteId)}?fields=${enc(fields)}"))
        val url = if (metadata.isNativeDocument) {
            val mime = CloudDocumentPolicy.exportMime(metadata.mimeType) ?: error("No supported document export")
            "${item(remoteId)}/export?mimeType=${enc(mime)}"
        } else "${item(remoteId)}?alt=media"
        validate(http.stream(url, token(), target, cancel, progress)); Unit
    }
    override suspend fun upload(parentId: String, name: String, mimeType: String, source: InputStream, length: Long?, cancel: CloudCancellation, progress: (Long) -> Unit) = runCatching {
        require(length != null && length >= 0) { "Known source size required" }
        val metadata = JSONObject().put("name", name).put("parents", org.json.JSONArray().put(parentId))
        val reply = validate(http.requestHeaders("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=${enc(fields)}",
            token(), metadata.toString().toByteArray(), "application/json", mapOf("X-Upload-Content-Type" to mimeType, "X-Upload-Content-Length" to length.toString())))
        val session = reply.location ?: error("Missing upload session")
        require(CloudUploadPolicy.safeSession(session) && java.net.URI(session).host == "www.googleapis.com")
        var sent = 0L
        var result: CloudEntry? = null
        do {
            currentCoroutineContext().ensureActive()
            if (cancel.isCancelled()) throw CancellationException("Upload cancelled")
            val chunk = source.cloudChunk(minOf(CloudUploadPolicy.DRIVE_CHUNK.toLong(), length - sent).toInt())
            check(chunk.isNotEmpty() || length == 0L) { "Source truncated" }
            val end = sent + chunk.size
            val range = if (length == 0L) "bytes */0" else "bytes $sent-${end - 1}/$length"
            val part = http.requestHeaders("PUT", session, token(), chunk, mimeType, mapOf("Content-Range" to range))
            if (part.status == 308) {
                check(end < length && part.range == "bytes=0-${end - 1}") { "Upload range was not confirmed" }
            }
            else result = parse(JSONObject(validate(part).body))
            sent = end
            progress(sent)
        } while (sent < length && result == null)
        check(sent == length && source.read() == -1) { "Source size changed" }
        (result ?: error("Upload not committed")).also { check(it.sizeBytes == length) { "Uploaded size not verified" } }
    }
    override suspend fun createFolder(parentId: String, name: String) = runCatching {
        parse(json("POST", "$base?fields=${enc(fields)}", JSONObject().put("name", name)
            .put("mimeType", "application/vnd.google-apps.folder").put("parents", org.json.JSONArray().put(parentId))))
    }
    override suspend fun rename(remoteId: String, newName: String) = runCatching {
        parse(json("PATCH", "${item(remoteId)}?fields=${enc(fields)}", JSONObject().put("name", newName)))
    }
    override suspend fun move(remoteId: String, newParentId: String) = runCatching {
        val old = json("GET", "${item(remoteId)}?fields=parents").optJSONArray("parents")
        val parents = old?.let { (0 until it.length()).map(it::getString).joinToString(",") }.orEmpty()
        parse(json("PATCH", "${item(remoteId)}?addParents=${enc(newParentId)}&removeParents=${enc(parents)}&fields=${enc(fields)}", JSONObject()))
    }
    override suspend fun copy(remoteId: String, newParentId: String) = runCatching {
        parse(json("POST", "${item(remoteId)}/copy?fields=${enc(fields)}", JSONObject().put("parents", org.json.JSONArray().put(newParentId))))
    }
    override suspend fun trash(remoteId: String) = runCatching { json("PATCH", item(remoteId), JSONObject().put("trashed", true)); Unit }
    override suspend fun disconnect() { disconnected = true; connectionState = CloudConnectionState.DISCONNECTED }
}
