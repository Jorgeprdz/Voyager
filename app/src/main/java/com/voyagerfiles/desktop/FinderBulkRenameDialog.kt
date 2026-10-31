package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R

/**
 * Preview is authoritative: no mutation until user confirms all target names.
 * This is initially local-only. Errors prevent the entire batch from starting.
 */
@Composable
internal fun FinderBulkRenameDialog(
    items: List<FinderRenameCandidate>,
    siblings: Set<String>,
    onDismiss: () -> Unit,
    onApply: (List<Pair<String, String>>) -> Unit,
) {
    var prefix by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf(false) }
    val preview = remember(items, siblings, prefix, number) {
        FinderBulkRenamePlanner.preview(items, siblings,
            FinderBulkRenameOption(
                rule = if (number) FinderRenameRule.NUMBER else FinderRenameRule.PREFIX,
                value = prefix
            )
        )
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_bulk_rename_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 450.dp)
                .verticalScroll(rememberScrollState())) {
                Row {
                    TextButton(onClick = { number = false }) {
                        Text(stringResource(R.string.finderdex_bulk_prefix))
                    }
                    TextButton(onClick = { number = true }) {
                        Text(stringResource(R.string.finderdex_bulk_number))
                    }
                }
                if (!number) OutlinedTextField(prefix, { prefix = it },
                    label = { Text(stringResource(R.string.finderdex_bulk_prefix)) },
                    singleLine = true)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.finderdex_bulk_preview))
                preview.rows.take(40).forEach { row ->
                    Text("${row.oldName} → ${row.newName}")
                    if (row.error != null) Text(stringResource(R.string.finderdex_bulk_invalid))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preview.valid && preview.changed > 0, onClick = {
                onApply(preview.rows.filter { it.oldName != it.newName }
                    .map { it.path to it.newName })
                onDismiss()
            }) { Text(stringResource(R.string.action_rename)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
