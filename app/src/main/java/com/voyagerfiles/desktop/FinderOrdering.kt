package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileArrangement
import com.voyagerfiles.data.model.FileArrangementEngine
import com.voyagerfiles.data.model.FileGroup
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.GroupBy
import com.voyagerfiles.data.model.SortBy
import com.voyagerfiles.data.model.SortOrder

/** One ordering policy shared with the mobile browser; never mutates the selected paths. */
internal fun finderOrderFiles(
    files: List<FileItem>,
    sort: SortBy = SortBy.NAME,
    descending: Boolean = false,
    groupFolders: Boolean = true,
): List<FileItem> = FileArrangementEngine.order(files, FileArrangement(
    sortBy = sort, sortOrder = if (descending) SortOrder.DESCENDING else SortOrder.ASCENDING,
    foldersFirst = groupFolders,
))

internal fun finderGroups(
    files: List<FileItem>,
    sort: SortBy = SortBy.NAME,
    descending: Boolean = false,
    groupBy: GroupBy = GroupBy.NONE,
    groupDescending: Boolean = false,
): List<FileGroup> = FileArrangementEngine.groups(files, FileArrangement(
    sortBy = sort, sortOrder = if (descending) SortOrder.DESCENDING else SortOrder.ASCENDING,
    groupBy = groupBy, groupOrder = if (groupDescending) SortOrder.DESCENDING else SortOrder.ASCENDING,
))

internal fun finderFlatten(groups: List<FileGroup>): List<FileItem> = groups.flatMap { it.files }
