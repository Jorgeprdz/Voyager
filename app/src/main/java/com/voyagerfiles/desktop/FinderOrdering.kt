package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.SortBy

/** Finder ordering never changes item identity or the authoritative selection. */
internal fun finderOrderFiles(
    files: List<FileItem>,
    sort: SortBy = SortBy.NAME,
    descending: Boolean = false,
    groupFolders: Boolean = true,
): List<FileItem> {
    val comparator: Comparator<FileItem> = when (sort) {
        SortBy.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        SortBy.DATE -> compareBy { it.lastModified.time }
        SortBy.SIZE -> compareBy { it.size }
        SortBy.TYPE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.extension }
    }
    val ordered = files.sortedWith(if (descending) comparator.reversed() else comparator)
    return if (groupFolders) ordered.sortedWith(compareBy { !it.isDirectory }) else ordered
}
