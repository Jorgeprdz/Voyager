package com.voyagerfiles.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.voyagerfiles.R
import com.voyagerfiles.data.model.GroupBy
import com.voyagerfiles.data.model.SortBy
import com.voyagerfiles.data.model.SortOrder

@Composable
fun FileSortMenu(
    sortBy: SortBy,
    sortOrder: SortOrder,
    onSortByChange: (SortBy) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    groupBy: GroupBy = GroupBy.NONE,
    groupOrder: SortOrder = SortOrder.ASCENDING,
    onGroupByChange: (GroupBy) -> Unit = {},
    onGroupOrderChange: (SortOrder) -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showSortMenu = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.content_desc_sort))
        }
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false },
        ) {
            SortBy.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    trailingIcon = {
                        if (sortBy == sort) {
                            Icon(
                                if (sortOrder == SortOrder.ASCENDING) Icons.Filled.KeyboardArrowUp
                                else Icons.Filled.KeyboardArrowDown,
                                stringResource(
                                    if (sortOrder == SortOrder.ASCENDING) {
                                        R.string.browser_sort_ascending
                                    } else {
                                        R.string.browser_sort_descending
                                    },
                                ),
                            )
                        }
                    },
                    onClick = {
                        onSortByChange(sort)
                        showSortMenu = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.finderdex_group_by)) },
                enabled = false, onClick = {},
            )
            GroupBy.entries.forEach { group ->
                val name = when (group) {
                    GroupBy.NONE -> R.string.finderdex_group_none
                    GroupBy.NAME -> R.string.browser_sort_name
                    GroupBy.DATE -> R.string.finderdex_modified_date
                    GroupBy.TYPE -> R.string.browser_sort_type
                    GroupBy.SIZE -> R.string.browser_sort_size
                }
                DropdownMenuItem(
                    text = { Text(stringResource(name)) },
                    trailingIcon = {
                        if (group == groupBy) Icon(
                            if (groupOrder == SortOrder.ASCENDING) Icons.Filled.KeyboardArrowUp
                            else Icons.Filled.KeyboardArrowDown, null)
                    },
                    onClick = { onGroupByChange(group); showSortMenu = false },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(
                    if (groupOrder == SortOrder.ASCENDING) R.string.finderdex_groups_descending
                    else R.string.finderdex_groups_ascending
                )) },
                enabled = groupBy != GroupBy.NONE,
                onClick = {
                    onGroupOrderChange(if (groupOrder == SortOrder.ASCENDING) SortOrder.DESCENDING
                        else SortOrder.ASCENDING)
                    showSortMenu = false
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (sortOrder == SortOrder.ASCENDING) {
                                R.string.browser_sort_descending_action
                            } else {
                                R.string.browser_sort_ascending_action
                            },
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        if (sortOrder == SortOrder.ASCENDING) Icons.Filled.KeyboardArrowDown
                        else Icons.Filled.KeyboardArrowUp,
                        null,
                    )
                },
                onClick = {
                    onSortOrderChange(
                        if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING
                        else SortOrder.ASCENDING,
                    )
                    showSortMenu = false
                },
            )
        }
    }
}
