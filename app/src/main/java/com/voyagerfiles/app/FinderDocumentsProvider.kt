package com.voyagerfiles.app

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.system.Os
import android.system.OsConstants
import android.webkit.MimeTypeMap
import com.voyagerfiles.util.DocumentPathPolicy
import com.voyagerfiles.util.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

/** Participates in Android's system picker. Only currently authorized shared volumes are exposed. */
class FinderDocumentsProvider : DocumentsProvider() {
    override fun onCreate() = true
    private fun roots(): Map<String, Pair<String, File>> {
        val ctx = requireNotNull(context)
        val permitted = if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
            else ctx.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!permitted) return emptyMap()
        return FileUtils.getStorageDirectories(ctx).mapNotNull { volume ->
            val file = runCatching { File(volume.path).canonicalFile }.getOrNull() ?: return@mapNotNull null
            if (!file.isDirectory || !file.canRead()) return@mapNotNull null
            val id = MessageDigest.getInstance("SHA-256").digest(file.path.toByteArray())
                .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
            id to (volume.name to file)
        }.toMap()
    }
    private fun file(id: String): File {
        val separator = id.indexOf(':')
        if (separator < 1) throw FileNotFoundException("Invalid document")
        val root = roots()[id.substring(0, separator)]?.second ?: throw FileNotFoundException("Storage unavailable")
        return runCatching { DocumentPathPolicy.resolve(root, id.substring(separator + 1)) }
            .getOrElse { throw FileNotFoundException("Document outside accessible storage") }
    }
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: arrayOf(
        Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES,
    )).apply {
        roots().forEach { (id, entry) ->
            addRow(columnNames.map<String, Any?> { column -> when (column) {
                Root.COLUMN_ROOT_ID -> id
                Root.COLUMN_DOCUMENT_ID -> "$id:"
                Root.COLUMN_TITLE -> "FinderDex · ${entry.first}"
                Root.COLUMN_FLAGS -> if (entry.second.canWrite()) Root.FLAG_SUPPORTS_CREATE else 0
                Root.COLUMN_MIME_TYPES -> "*/*"
                else -> null
            } }.toTypedArray())
        }
    }
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = cursor(projection).apply {
        addDocument(documentId, file(documentId))
    }
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor =
        cursor(projection).apply {
            val parent = file(parentDocumentId)
            if (!parent.isDirectory) throw FileNotFoundException("Not a directory")
            parent.listFiles()?.forEach { child ->
                val id = parentDocumentId.trimEnd('/') + (if (parentDocumentId.endsWith(':')) "" else "/") + child.name
                runCatching { file(id) }.getOrNull()?.takeIf { it.canRead() }?.let { addDocument(id, it) }
            } ?: throw FileNotFoundException("Directory unavailable")
        }
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean = runCatching {
        if (parentDocumentId.substringBefore(':') != documentId.substringBefore(':')) return false
        val parent = file(parentDocumentId)
        val child = file(documentId)
        child != parent && child.toPath().startsWith(parent.toPath())
    }.getOrDefault(false)

    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        require(DocumentPathPolicy.validName(displayName)) { "Invalid file name" }
        val parent = file(parentDocumentId)
        if (!parent.isDirectory || !parent.canWrite()) throw FileNotFoundException("Destination unavailable")
        val id = parentDocumentId.trimEnd('/') + (if (parentDocumentId.endsWith(':')) "" else "/") + displayName
        val descriptor = Os.open(parent.path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK or OsConstants.O_NOFOLLOW, 0)
        try {
            check(OsConstants.S_ISDIR(Os.fstat(descriptor).st_mode)) { "Not a directory" }
            ParcelFileDescriptor.dup(descriptor).use { heldParent ->
                val actualParent = File(Os.readlink("/proc/self/fd/${heldParent.fd}")).canonicalFile
                check(actualParent == parent && actualParent == file(parentDocumentId)) { "Destination changed" }
                // A held directory descriptor prevents a replaced pathname from redirecting creation.
                val child = "/proc/self/fd/${heldParent.fd}/$displayName"
                if (mimeType == Document.MIME_TYPE_DIR) Os.mkdir(child, 448)
                else Os.close(Os.open(child, OsConstants.O_WRONLY or OsConstants.O_CREAT or
                    OsConstants.O_EXCL or OsConstants.O_NOFOLLOW, 384))
            }
        } finally { Os.close(descriptor) }
        return id
    }
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        signal?.throwIfCanceled()
        val target = file(documentId)
        if (!target.isFile) throw FileNotFoundException("Not a regular file")
        val flags = when (mode) {
            "r" -> OsConstants.O_RDONLY
            "w", "wt", "wa" -> OsConstants.O_WRONLY
            "rw", "rwt" -> OsConstants.O_RDWR
            else -> throw IllegalArgumentException("Unsupported access mode")
        } or OsConstants.O_NOFOLLOW or (if (mode == "wa") OsConstants.O_APPEND else 0)
        val descriptor = Os.open(target.path, flags, 0)
        try {
            // Validate the opened inode before truncation, including a concurrently changed parent.
            val pfd = ParcelFileDescriptor.dup(descriptor)
            try {
                val actual = File(Os.readlink("/proc/self/fd/${pfd.fd}")).canonicalFile
                check(actual == target && actual == file(documentId)) { "Document changed while opening" }
                signal?.throwIfCanceled()
                if (mode in setOf("w", "wt", "rwt")) Os.ftruncate(descriptor, 0)
                return pfd
            } catch (error: Throwable) { pfd.close(); throw error }
        } finally { Os.close(descriptor) }
    }
    private fun cursor(projection: Array<out String>?) = MatrixCursor(projection ?: arrayOf(
        Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
        Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
    ))
    private fun MatrixCursor.addDocument(id: String, target: File) {
        if (!target.exists()) throw FileNotFoundException("Document unavailable")
        addRow(columnNames.map<String, Any?> { column -> when (column) {
            Document.COLUMN_DOCUMENT_ID -> id
            Document.COLUMN_DISPLAY_NAME -> target.name
            Document.COLUMN_MIME_TYPE -> if (target.isDirectory) Document.MIME_TYPE_DIR
                else MimeTypeMap.getSingleton().getMimeTypeFromExtension(target.extension.lowercase()) ?: "application/octet-stream"
            Document.COLUMN_SIZE -> if (target.isDirectory) null else target.length()
            Document.COLUMN_LAST_MODIFIED -> target.lastModified()
            Document.COLUMN_FLAGS -> if (!target.canWrite()) 0 else if (target.isDirectory) Document.FLAG_DIR_SUPPORTS_CREATE else Document.FLAG_SUPPORTS_WRITE
            else -> null
        } }.toTypedArray())
    }
}
