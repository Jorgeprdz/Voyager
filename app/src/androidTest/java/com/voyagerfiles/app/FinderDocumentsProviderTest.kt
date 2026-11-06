package com.voyagerfiles.app

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.UUID

class FinderDocumentsProviderTest {
    @Test fun pickerCanCreateAndReadAndRejectTraversalWithoutOverwrite() {
        assumeTrue(Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val authority = "${context.packageName}.documents"
        val root = resolver.query(DocumentsContract.buildRootsUri(authority), arrayOf(Root.COLUMN_DOCUMENT_ID),
            null, null, null)!!.use { cursor ->
            assertTrue(cursor.moveToFirst()); cursor.getString(0)
        }
        val provider = FinderDocumentsProvider().apply {
            attachInfo(context, android.content.pm.ProviderInfo().apply { this.authority = authority })
        }
        val rootUri = DocumentsContract.buildDocumentUri(authority, root)
        val folderName = ".finderdex-picker-test-${UUID.randomUUID()}"
        val folder = DocumentsContract.createDocument(resolver, rootUri, Document.MIME_TYPE_DIR, folderName)!!
        try {
            val uri = DocumentsContract.createDocument(resolver, folder, "text/plain", "test.txt")!!
            resolver.openOutputStream(uri, "wt")!!.use { it.write("FinderDex".toByteArray()) }
            assertEquals("FinderDex", resolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            assertTrue(runCatching { provider.createDocument(DocumentsContract.getDocumentId(folder), "text/plain", "test.txt") }.isFailure)
            assertEquals("FinderDex", resolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
            assertTrue(runCatching { provider.queryDocument(root + "../escape", null) }.isFailure)
        } finally {
            // This is exclusively the UUID-named test directory created above.
            File(Environment.getExternalStorageDirectory(), folderName).deleteRecursively()
        }
    }
}
