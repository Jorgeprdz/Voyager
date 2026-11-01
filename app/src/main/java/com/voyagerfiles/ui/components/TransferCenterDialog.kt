package com.voyagerfiles.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.ui.text.asString
import com.voyagerfiles.viewmodel.*

/** Uses actual coordinator state. Does not offer unsupported pause or replay destructive work. */
@Composable
internal fun TransferCenterDialog(viewModel: FileBrowserViewModel, onDismiss: () -> Unit) {
    val operation by viewModel.operationState.collectAsState()
    val renames by viewModel.renameResults.collectAsState()
    val history by viewModel.transferHistory.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.transfer_channel)) },
        text = { Column(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            val running = operation as? OperationState.Running
            if (running != null) {
                Text(running.progress.label.asString())
                val fraction = running.progress.fraction
                if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(running.progress.detailText.orEmpty())
                TextButton(enabled = running.cancellable && !running.cancelling,
                    onClick = { viewModel.cancelOperation() }) { Text(stringResource(R.string.action_cancel)) }
            }
            if (history.isEmpty() && running == null) Text(stringResource(R.string.r31_empty))
            LazyColumn {
                itemsIndexed(renames) { _, entry ->
                    Text((if (entry.second) "✓ " else "! ") + entry.first, Modifier.padding(vertical = 6.dp),
                        color = if (entry.second) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                }
                itemsIndexed(history) { _, result ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(result.progress.label.asString(), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(when (result.outcome) {
                            OperationOutcome.COMPLETED -> R.string.transfer_completed
                            OperationOutcome.CANCELLED -> R.string.transfer_cancelled
                            OperationOutcome.FAILED -> R.string.transfer_finished_with_errors
                        }))
                        Text(result.progress.detailText.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.finderdex_close)) } })
}
