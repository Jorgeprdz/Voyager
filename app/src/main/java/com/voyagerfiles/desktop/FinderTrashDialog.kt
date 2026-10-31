package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voyagerfiles.data.model.TrashEntry
import java.text.DateFormat
import java.util.Date

/** Lists the genuine Voyager trash index. Restoration is delegated to its guarded ViewModel. */
@Composable
internal fun FinderTrashDialog(
    entries: List<TrashEntry>,
    loading: Boolean,
    error: String?,
    onRestore: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_trash_title)) },
        text = {
            Column {
                if (loading) Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_trash_loading))
                if (error != null) Text(error)
                if (!loading && error == null && entries.isEmpty()) Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_trash_empty))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(entries, key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.originalPath, fontSize = 10.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis)
                                Text(DateFormat.getDateTimeInstance().format(Date(item.deletedAt)),
                                    fontSize = 10.sp)
                            }
                            TextButton(onClick = { onRestore(item.id) }) { Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_restore)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_close)) } },
        dismissButton = { TextButton(onClick = onRefresh) { Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_refresh)) } },
    )
}
