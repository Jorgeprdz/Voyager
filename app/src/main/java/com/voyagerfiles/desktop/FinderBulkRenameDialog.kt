package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
    var rule by rememberSaveable { mutableStateOf(FinderRenameRule.PREFIX) }
    var value by rememberSaveable { mutableStateOf("") }
    var replacement by rememberSaveable { mutableStateOf("") }
    var first by rememberSaveable { mutableStateOf("1") }
    var digits by rememberSaveable { mutableStateOf("3") }
    var preserveExtension by rememberSaveable { mutableStateOf(true) }
    val validNumber = first.toIntOrNull()?.let { it >= 0 && it <= Int.MAX_VALUE - items.size } == true && digits.toIntOrNull() in 1..8
    val preview = remember(items, siblings, rule, value, replacement, first, digits, preserveExtension) {
        FinderBulkRenamePlanner.preview(items, siblings, FinderBulkRenameOption(rule, value,
            replacement, first.toIntOrNull()?.coerceIn(0, Int.MAX_VALUE - items.size) ?: 1,
            digits.toIntOrNull()?.coerceIn(1, 8) ?: 3, !preserveExtension))
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_bulk_rename_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 450.dp)
                .verticalScroll(rememberScrollState())) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) { Text(stringResource(renameRuleLabel(rule))) }
                    DropdownMenu(expanded, { expanded = false }) {
                        FinderRenameRule.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(stringResource(renameRuleLabel(option))) },
                                onClick = { rule = option; expanded = false })
                        }
                    }
                }
                if (rule in listOf(FinderRenameRule.PREFIX, FinderRenameRule.SUFFIX, FinderRenameRule.REPLACE))
                    OutlinedTextField(value, { value = it }, label = { Text(stringResource(renameRuleLabel(rule))) }, singleLine = true)
                if (rule == FinderRenameRule.REPLACE)
                    OutlinedTextField(replacement, { replacement = it }, label = { Text(stringResource(R.string.r31_replace_with)) }, singleLine = true)
                if (rule == FinderRenameRule.NUMBER) Row {
                    OutlinedTextField(first, { first = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.r31_first_number)) }, singleLine = true, isError = !validNumber)
                    OutlinedTextField(digits, { digits = it }, Modifier.weight(1f), label = { Text(stringResource(R.string.r31_digits)) }, singleLine = true, isError = !validNumber)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(preserveExtension, { preserveExtension = it })
                    Text(stringResource(R.string.r31_preserve_extension))
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.finderdex_bulk_preview))
                preview.rows.forEach { row ->
                    Text("${row.oldName} → ${row.newName}")
                    if (row.error != null) Text("${stringResource(R.string.finderdex_bulk_invalid)}: ${row.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preview.valid && preview.changed > 0 && (rule != FinderRenameRule.NUMBER || validNumber), onClick = {
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

private fun renameRuleLabel(rule: FinderRenameRule): Int = when (rule) {
    FinderRenameRule.PREFIX -> R.string.finderdex_bulk_prefix
    FinderRenameRule.NUMBER -> R.string.finderdex_bulk_number
    FinderRenameRule.SUFFIX -> R.string.r31_suffix
    FinderRenameRule.REPLACE -> R.string.r31_replace
    FinderRenameRule.UPPER -> R.string.r31_upper
    FinderRenameRule.LOWER -> R.string.r31_lower
}
