package com.voyagerfiles.data.archive

/** Android's archive MIME labels vary by producer. Never register a broad octet-stream handler. */
object ExternalArchiveIntent {
    private val suffixByMime = mapOf(
        "application/zip" to ".zip",
        "application/x-zip-compressed" to ".zip",
        "application/x-tar" to ".tar",
        "application/x-gtar" to ".tar",
        "application/gzip" to ".gz",
        "application/x-gzip" to ".gz",
        "application/x-compressed-tar" to ".tar.gz",
        "application/x-tgz" to ".tgz",
        "application/x-bzip2" to ".bz2",
        "application/x-rar-compressed" to ".rar",
        "application/vnd.rar" to ".rar",
        "application/x-rar" to ".rar",
    )

    fun isSupportedUri(scheme: String?, mime: String?): Boolean =
        scheme == "content" && mime?.lowercase() in suffixByMime

    fun archiveName(displayName: String?, mime: String?): String? {
        val name = displayName?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }
            ?: "archive"
        if (ArchiveFormat.detect(name) != null) return name
        val suffix = suffixByMime[mime?.lowercase()] ?: return null
        // A non-archive suffix must not become an ambiguous doubled extension.
        if (name.contains('.') && name.substringAfterLast('.').isNotBlank()) return null
        return name + suffix
    }
}
