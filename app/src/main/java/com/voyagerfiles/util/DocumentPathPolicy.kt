package com.voyagerfiles.util

import java.io.File

/** Document IDs contain a root key and a relative path, never an arbitrary absolute path. */
object DocumentPathPolicy {
    fun resolve(root: File, relative: String): File {
        require(!File(relative).isAbsolute && relative.split('/').none { it == ".." } && '\u0000' !in relative)
        val base = root.canonicalFile
        val file = File(base, relative).canonicalFile
        require(file.toPath().startsWith(base.toPath())) { "Document outside storage root" }
        // Android protected trees must not be re-exported through all-files access.
        val components = file.relativeTo(base).invariantSeparatorsPath.split('/')
        require(!(components.firstOrNull().equals("Android", ignoreCase = true) &&
            components.getOrNull(1)?.lowercase(java.util.Locale.ROOT) in setOf("data", "obb")))
        return file
    }
    fun validName(name: String): Boolean = name.isNotBlank() && name !in setOf(".", "..") &&
        name.toByteArray(Charsets.UTF_8).size <= 255 && name.none { it == '/' || it == '\\' || it.code < 32 }
}
