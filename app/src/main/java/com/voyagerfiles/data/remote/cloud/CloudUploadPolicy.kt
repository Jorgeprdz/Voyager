package com.voyagerfiles.data.remote.cloud

import java.net.URI

internal object CloudUploadPolicy {
    const val DRIVE_CHUNK = 262144 * 12
    const val CHUNK = 327680 * 10
    fun safeSession(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            uri.port == -1 && uri.fragment == null
    }.getOrDefault(false)
    fun nextOffset(ranges: List<String>, length: Long): Long {
        val first = ranges.map { it.substringBefore('-').toLong() }.minOrNull() ?: error("Missing upload range")
        require(first in 0 until length)
        return first
    }
}

internal fun java.io.InputStream.cloudChunk(maximum: Int): ByteArray {
    val bytes = ByteArray(maximum)
    var read = 0
    while (read < maximum) {
        val count = read(bytes, read, maximum - read)
        if (count < 0) break
        if (count == 0) continue
        read += count
    }
    return if (read == maximum) bytes else bytes.copyOf(read)
}
