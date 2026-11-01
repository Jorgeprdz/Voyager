package com.voyagerfiles.data.index

import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.FileTypeFilter

/**
 * Pure aggregation over StorageCategoryIndex.scanAll's bounded, permission-aware snapshot.
 * Values describe scanned files only; inaccessible areas and app-private storage are unknown.
 * No file deletion, file hashing, or transmission occurs here.
 */
data class StorageAnalyticsSummary(
    val scannedFiles: Int,
    val scannedBytes: Long,
    val categories: List<CategoryBytes>,
    val largestFiles: List<FileItem>,
    val largestFolders: List<FolderBytes>,
) {
    data class CategoryBytes(val category: String, val bytes: Long, val files: Int)
    data class FolderBytes(val path: String, val bytes: Long, val files: Int)

    companion object {
        fun fromFiles(files: List<FileItem>, limit: Int = 20): StorageAnalyticsSummary {
            require(limit in 1..100)
            val unique = files.asSequence().filter { !it.isDirectory }
                .distinctBy { it.path }.toList()
            val total = unique.sumOf { it.size.coerceAtLeast(0L) }
            val categories = unique.groupBy { item ->
                when {
                    FileTypeFilter.VIDEOS.matches(item) -> "video"
                    FileTypeFilter.IMAGES.matches(item) -> "image"
                    FileTypeFilter.AUDIO.matches(item) -> "audio"
                    FileTypeFilter.DOCUMENTS.matches(item) -> "document"
                    FileTypeFilter.ARCHIVES.matches(item) -> "archive"
                    FileTypeFilter.APPS.matches(item) -> "app"
                    else -> "other"
                }
            }.map { (group, items) ->
                CategoryBytes(group, items.sumOf { it.size.coerceAtLeast(0L) }, items.size)
            }.sortedWith(compareByDescending<CategoryBytes> { it.bytes }.thenBy { it.category })
            // Only immediate parent totals: no fake recursive disk usage from missing files.
            val folders = unique.groupBy { it.path.substringBeforeLast('/', "") }
                .map { (folder, items) ->
                    FolderBytes(folder, items.sumOf { it.size.coerceAtLeast(0L) }, items.size)
                }.sortedWith(compareByDescending<FolderBytes> { it.bytes }.thenBy { it.path })
                .take(limit)
            return StorageAnalyticsSummary(
                scannedFiles = unique.size,
                scannedBytes = total,
                categories = categories,
                largestFiles = unique.sortedWith(
                    compareByDescending<FileItem> { it.size }.thenBy { it.path }
                ).take(limit),
                largestFolders = folders,
            )
        }
    }
}
