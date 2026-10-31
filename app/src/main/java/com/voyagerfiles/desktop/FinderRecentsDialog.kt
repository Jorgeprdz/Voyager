package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val modified = remember(lastModifiedInCurrentFolder) {
        lastModifiedInCurrentFolder.filter { !it.isDirectory }
            .sortedByDescending { it.lastModified.time }.take(30)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_recents_title)) },
        text = {
            Column(Modifier.heightIn(max = 500.dp)) {
                Text(stringResource(R.string.finderdex_recents_opened))
                if (opened.isEmpty()) Text(stringResource(R.string.finderdex_recents_empty))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(opened, key = { it.path }) { item ->
                        TextButton(onClick = { onOpenLocation(File(item.path).parent ?: "/") }) {
                            Text(stringResource(R.string.finderdex_recents_entry,
                                File(item.path).name,
                                DateFormat.getDateTimeInstance().format(Date(item.openedAt))))
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
