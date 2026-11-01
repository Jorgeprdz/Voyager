package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.data.model.FileItem
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
internal fun FinderRecentsDialog(
    lastModifiedInCurrentFolder: List<FileItem>,
    onDismiss: () -> Unit,
    onOpenLocation: (String) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { FinderRecentStore(context) }
    var opened by remember { mutableStateOf(store.list()) }
    var query by remember { mutableStateOf("") }
    var recentWeek by remember { mutableStateOf(false) }
    val filtered = opened.filter { (query.isBlank() || it.path.contains(query, ignoreCase = true)) &&
        (!recentWeek || it.openedAt >= System.currentTimeMillis() - 7L * 86400000) }
    val modified = remember(lastModifiedInCurrentFolder) {
        lastModifiedInCurrentFolder.filter { !it.isDirectory }
            .sortedByDescending { it.lastModified.time }.take(30)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_recents_title)) },
        text = {
            Column(Modifier.heightIn(max = 500.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.action_search)) }, singleLine = true)
                Row {
                    androidx.compose.material3.Checkbox(recentWeek, { recentWeek = it })
                    Text(stringResource(R.string.r31_last_week))
                }
                Text(stringResource(R.string.finderdex_recents_opened))
                if (filtered.isEmpty()) Text(stringResource(R.string.finderdex_recents_empty))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it.path }) { item ->
                        Row {
                            TextButton(modifier = Modifier.weight(1f), onClick = { onOpenLocation(File(item.path).parent ?: "/") }) {
                                Text(stringResource(R.string.finderdex_recents_entry,
                                    File(item.path).name,
                                    DateFormat.getDateTimeInstance().format(Date(item.openedAt))))
                            }
                            TextButton(onClick = { store.remove(item.path); opened = store.list() }) {
                                Text(stringResource(R.string.r31_remove_history))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.finderdex_recents_modified))
                if (modified.isEmpty()) Text(stringResource(R.string.finderdex_recents_empty))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(modified, key = { it.path }) { item ->
                        TextButton(onClick = { onOpenLocation(File(item.path).parent ?: "/") }) {
                            Text(item.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.finderdex_close)) }
        },
        dismissButton = {
            TextButton(onClick = { store.clear(); opened = emptyList() }) {
                Text(stringResource(R.string.finderdex_recents_clear))
            }
        },
    )
}
