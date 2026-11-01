package com.voyagerfiles.data.archive

import org.junit.Assert.*
import org.junit.Test

class ExternalArchiveIntentTest {
    @Test fun compoundExtensionsRemainIntact() {
        assertEquals("backup.tar.gz", ExternalArchiveIntent.archiveName("backup.tar.gz", "application/gzip"))
        assertEquals(ArchiveFormat.TAR_GZIP, ArchiveFormat.detect(
            ExternalArchiveIntent.archiveName("backup.tar.gz", "application/gzip")!!
        ))
    }

    @Test fun externalHandlerRejectsUnrelatedMimeAndFileUris() {
        assertTrue(ExternalArchiveIntent.isSupportedUri("content", "application/zip"))
        assertFalse(ExternalArchiveIntent.isSupportedUri("content", "application/octet-stream"))
        assertFalse(ExternalArchiveIntent.isSupportedUri("file", "application/zip"))
        assertNull(ExternalArchiveIntent.archiveName("invoice.pdf", "application/zip"))
    }

    @Test fun rarIsRecognizedButNotAdvertisedAsExtractable() {
        val name = ExternalArchiveIntent.archiveName("backup", "application/vnd.rar")!!
        assertEquals(ArchiveFormat.RAR_UNSUPPORTED, ArchiveFormat.detect(name))
        assertFalse(ArchiveFormat.detect(name)!!.canExtract)
    }
}
