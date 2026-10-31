package com.voyagerfiles.data.index

import com.voyagerfiles.data.model.FileItem
import org.junit.Assert.*
import org.junit.Test

class StorageAnalyticsSummaryTest {
    private fun file(path: String, size: Long) =
        FileItem(name = path.substringAfterLast('/'), path = path, isDirectory = false, size = size)

    @Test fun totalsCountUniqueFilePathsOnly() {
        val items = listOf(file("/root/A/file.mp4", 300), file("/root/A/file.mp4", 300),
            file("/root/B/file.pdf", 100), file("/root/A/photo.jpg", 25))
        val result = StorageAnalyticsSummary.fromFiles(items)
        assertEquals(3, result.scannedFiles)
        assertEquals(425L, result.scannedBytes)
        assertEquals(2, result.largestFolders.size)
        assertEquals(325L, result.largestFolders.first().bytes)
        assertEquals("/root/A", result.largestFolders.first().path)
        assertEquals("file.mp4", result.largestFiles.first().name)
        assertEquals(3, result.categories.sumOf { it.files })
    }

    @Test fun emptyAndBounded() {
        assertEquals(0L, StorageAnalyticsSummary.fromFiles(emptyList()).scannedBytes)
        assertEquals(1, StorageAnalyticsSummary.fromFiles(
            listOf(file("/x/a", 7), file("/y/b", 9)), limit = 1).largestFolders.size)
        assertThrows(IllegalArgumentException::class.java) {
            StorageAnalyticsSummary.fromFiles(emptyList(), limit = 0)
        }
    }

    @Test fun negativeMetadataIsNotCountedAsUsedSpace() {
        val result = StorageAnalyticsSummary.fromFiles(listOf(file("/x/a", -1)))
        assertEquals(0L, result.scannedBytes)
    }
}
