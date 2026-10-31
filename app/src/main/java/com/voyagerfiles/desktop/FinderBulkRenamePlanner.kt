package com.voyagerfiles.desktop

import java.text.Normalizer
import java.util.Locale

/** Offline rename preview: never mutates paths. Execution must use the existing provider. */
internal enum class FinderRenameRule { PREFIX, SUFFIX, REPLACE, NUMBER, UPPER, LOWER }
internal data class FinderBulkRenameOption(
    val rule: FinderRenameRule,
    val value: String = "",
    val replaceWith: String = "",
    val firstNumber: Int = 1,
    val digits: Int = 3,
    val changeExtension: Boolean = false,
)
internal data class FinderRenameCandidate(val path: String, val name: String)
internal data class FinderRenameRow(
    val path: String,
    val oldName: String,
    val newName: String,
    val error: String? = null,
)
internal data class FinderRenamePreview(val rows: List<FinderRenameRow>) {
    val valid: Boolean get() = rows.isNotEmpty() && rows.all { it.error == null }
    val changed: Int get() = rows.count { it.newName != it.oldName && it.error == null }
}
internal object FinderBulkRenamePlanner {
    fun preview(
        items: List<FinderRenameCandidate>,
        siblingNames: Set<String>,
        option: FinderBulkRenameOption,
    ): FinderRenamePreview {
        require(items.size in 1..1000 && option.digits in 1..8)
        val ordered = items.distinctBy { it.path }.sortedBy { it.path }
        val oldNames = ordered.mapTo(mutableSetOf()) { it.name.lowercase(Locale.ROOT) }
        val collisions = siblingNames.map { it.lowercase(Locale.ROOT) }.toSet()
        val planned = ordered.mapIndexed { i, item ->
            val dot = item.name.lastIndexOf('.')
            val split = if (!option.changeExtension && dot > 0) dot else item.name.length
            val stem = item.name.substring(0, split)
            val extension = item.name.substring(split)
            val nextStem = when (option.rule) {
                FinderRenameRule.PREFIX -> option.value + stem
                FinderRenameRule.SUFFIX -> stem + option.value
                FinderRenameRule.REPLACE -> if (option.value.isEmpty()) stem else stem.replace(option.value, option.replaceWith)
                FinderRenameRule.NUMBER -> stem + " " + (option.firstNumber + i).toString().padStart(option.digits, '0')
                FinderRenameRule.UPPER -> stem.uppercase(Locale.ROOT)
                FinderRenameRule.LOWER -> stem.lowercase(Locale.ROOT)
            }
            val proposed = Normalizer.normalize(nextStem + extension, Normalizer.Form.NFC)
            val bytes = proposed.toByteArray(Charsets.UTF_8).size
            val error = when {
                proposed.isBlank() || proposed in setOf(".", "..") ||
                    proposed.any { it == '/' || it == '\\' || it.code in 0..31 } ->
                    "Invalid file name"
                bytes > 255 -> "File name exceeds 255 UTF-8 bytes"
                proposed.lowercase(Locale.ROOT) in collisions &&
                    proposed.lowercase(Locale.ROOT) !in oldNames -> "Existing sibling name"
                proposed.lowercase(Locale.ROOT) in oldNames &&
                    proposed != item.name -> "Conflicts with selected original"
                else -> null
            }
            FinderRenameRow(item.path, item.name, proposed, error)
        }
        val duplicates = planned.groupBy { it.newName.lowercase(Locale.ROOT) }.values
            .filter { it.size > 1 }.flatMap { group -> group.map { it.path } }.toSet()
        return FinderRenamePreview(planned.map {
            if (it.path in duplicates) it.copy(error = "Duplicate preview name") else it
        })
    }
}
