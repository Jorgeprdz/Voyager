package com.voyagerfiles.data.model

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.Locale

enum class GroupBy { NONE, NAME, DATE, TYPE, SIZE }

data class FileArrangement(
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val groupBy: GroupBy = GroupBy.NONE,
    val groupOrder: SortOrder = SortOrder.ASCENDING,
    val foldersFirst: Boolean = true,
)

data class FileGroup(
    val key: String,
    val heading: String,
    val files: List<FileItem>,
)

/**
 * One deterministic ordering/grouping engine for mobile, columns, icons, list and gallery.
 * The caller supplies the files that have actually been loaded; no implicit remote paging.
 */
object FileArrangementEngine {
    fun order(files: List<FileItem>, options: FileArrangement): List<FileItem> {
        val sort = compareByItem(options.sortBy)
        val directed = if (options.sortOrder == SortOrder.DESCENDING) sort.reversed() else sort
        return files.sortedWith(Comparator { a, b ->
            if (options.foldersFirst && a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else directed.compare(a, b)
        })
    }

    fun groups(files: List<FileItem>, options: FileArrangement, now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()): List<FileGroup> {
        if (options.groupBy == GroupBy.NONE) {
            return listOf(FileGroup("all", "", order(files, options)))
        }
        val mapped = files.groupBy { groupKey(it, options.groupBy, now, zone) }
        val orderedKeys = mapped.keys.sortedWith(compareBy<GroupKey> { it.rank }.thenBy { it.label })
            .let { if (options.groupOrder == SortOrder.DESCENDING) it.asReversed() else it }
        return orderedKeys.map { key ->
            FileGroup(key.id, key.label, order(mapped.getValue(key), options))
        }
    }

    data class GroupKey(val id: String, val label: String, val rank: Int)

    fun groupKey(file: FileItem, by: GroupBy, now: Instant, zone: ZoneId): GroupKey = when (by) {
        GroupBy.NONE -> GroupKey("all", "", 0)
        GroupBy.NAME -> {
            val c = fold(file.name).firstOrNull()
            val letter = c?.uppercaseChar()?.takeIf { it in 'A'..'Z' }?.toString()
                ?: if (c?.isDigit() == true) "0–9" else "#"
            GroupKey("initial:$letter", letter, when (letter) {
                "#" -> 0
                "0–9" -> 1
                else -> 2 + (letter[0] - 'A')
            })
        }
        GroupBy.DATE -> {
            val time = file.lastModified.time
            if (time <= 0L) GroupKey("unknown", "Fecha desconocida", Int.MAX_VALUE)
            else {
                val day = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
                val today = now.atZone(zone).toLocalDate()
                val thisWeek = today.minusDays(7)
                when {
                    day.isAfter(today) -> GroupKey("future", "Fecha futura", 0)
                    day == today -> GroupKey("today", "Hoy", 1)
                    day == today.minusDays(1) -> GroupKey("yesterday", "Ayer", 2)
                    !day.isBefore(thisWeek) -> GroupKey("last-week", "Últimos 7 días (sin hoy ni ayer)", 3)
                    day.year == today.year && day.month == today.month ->
                        GroupKey("month", "Este mes (anterior a 7 días)", 4)
                    day.year == today.year ->
                        GroupKey("year", "Este año (meses anteriores)", 5)
                    else -> GroupKey("old:"+day.year, day.year.toString(), 10000 - day.year)
                }
            }
        }
        GroupBy.TYPE -> when {
            file.isDirectory -> GroupKey("folder", "Carpetas", 0)
            file.isImage -> GroupKey("image", "Imágenes", 1)
            file.isVideo -> GroupKey("video", "Videos", 2)
            file.isAudio -> GroupKey("audio", "Audio", 3)
            file.isArchive -> GroupKey("archive", "Comprimidos", 4)
            file.isApk -> GroupKey("app", "Aplicaciones", 5)
            file.isText || file.isPdf || file.isOfficeDocument -> GroupKey("document", "Documentos", 6)
            file.extension.isBlank() -> GroupKey("no-extension", "Sin extensión", 7)
            else -> GroupKey("other", "Otros", 8)
        }
        GroupBy.SIZE -> when {
            file.isDirectory -> GroupKey("folder", "Carpetas (tamaño sin calcular)", -1)
            file.size < 0 -> GroupKey("unknown", "Tamaño desconocido", Int.MAX_VALUE)
            file.size < 1024 -> GroupKey("b0", "0 B – 1 KB", 0)
            file.size < 1024L * 1024 -> GroupKey("b1", "1 KB – 1 MB", 1)
            file.size < 1024L * 1024 * 1024 -> GroupKey("b2", "1 MB – 1 GB", 2)
            else -> GroupKey("b3", "1 GB o más", 3)
        }
    }

    fun compareNames(a: String, b: String): Int {
        val x = fold(a); val y = fold(b)
        var i = 0; var j = 0
        while (i < x.length && j < y.length) {
            if (x[i].isDigit() && y[j].isDigit()) {
                val ia = i; val jb = j
                while (i < x.length && x[i].isDigit()) i++
                while (j < y.length && y[j].isDigit()) j++
                val aa = x.substring(ia, i).trimStart('0').ifEmpty { "0" }
                val bb = y.substring(jb, j).trimStart('0').ifEmpty { "0" }
                val difference = aa.length.compareTo(bb.length).takeIf { it != 0 }
                    ?: aa.compareTo(bb).takeIf { it != 0 }
                    ?: (i-ia).compareTo(j-jb)
                if (difference != 0) return difference
            } else {
                val c = x[i].compareTo(y[j])
                if (c != 0) return c
                i++; j++
            }
        }
        return x.length.compareTo(y.length).takeIf { it != 0 }
            ?: a.lowercase(Locale.ROOT).compareTo(b.lowercase(Locale.ROOT)).takeIf { it != 0 }
            ?: a.compareTo(b)
    }

    private fun compareByItem(sort: SortBy): Comparator<FileItem> {
        val tie = Comparator<FileItem> { a, b ->
            compareNames(a.name, b.name).takeIf { it != 0 } ?: a.path.compareTo(b.path)
        }
        return Comparator { a, b ->
            val primary = when (sort) {
                SortBy.NAME -> 0
                SortBy.DATE -> compareNullableLong(a.lastModified.time.takeIf { it > 0 },
                    b.lastModified.time.takeIf { it > 0 })
                SortBy.SIZE -> compareNullableLong(a.size.takeUnless { a.isDirectory || it < 0 },
                    b.size.takeUnless { b.isDirectory || it < 0 })
                SortBy.TYPE -> typeLabel(a).compareTo(typeLabel(b))
            }
            if (primary == 0) tie.compare(a, b) else primary
        }
    }

    private fun compareNullableLong(a: Long?, b: Long?): Int = when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        else -> a.compareTo(b)
    }

    private fun typeLabel(item: FileItem): String = when {
        item.isDirectory -> ""
        item.extension.isBlank() -> "~"
        else -> fold(item.extension)
    }

    private fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
}
