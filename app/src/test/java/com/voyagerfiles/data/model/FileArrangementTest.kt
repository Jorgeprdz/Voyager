package com.voyagerfiles.data.model

import java.time.Instant
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class FileArrangementTest {
    private fun file(name: String, size: Long = 10, directory: Boolean = false, modified: Long = 1000L) =
        FileItem(name, "/test/$name", directory, size = size, lastModified = Date(modified))

    @Test fun naturalNameSortWorksInBothDirectionsWithAccents() {
        val files = listOf(file("archivo10.txt"), file("archivo2.txt"), file("Árchivo1.txt"))
        val ascending = FileArrangementEngine.order(files, FileArrangement()).map { it.name }
        assertEquals(listOf("Árchivo1.txt","archivo2.txt","archivo10.txt"), ascending)
        assertEquals(ascending.reversed(), FileArrangementEngine.order(files,
            FileArrangement(sortOrder = SortOrder.DESCENDING)).map { it.name })
    }

    @Test fun everySortCriterionSupportsBothDirections() {
        val items = listOf(
            file("beta.mp4", size = 90, modified = 3000),
            file("alpha.txt", size = 30, modified = 1000),
            file("zeta.jpg", size = 10, modified = 2000),
        )
        for (by in SortBy.entries) {
            val ascending = FileArrangementEngine.order(items, FileArrangement(sortBy = by))
            val descending = FileArrangementEngine.order(items, FileArrangement(sortBy = by,
                sortOrder = SortOrder.DESCENDING))
            assertEquals(items.map { it.path }.toSet(), ascending.map { it.path }.toSet())
            assertEquals(ascending.map { it.path }.reversed(), descending.map { it.path })
        }
    }

    @Test fun groupingDirectionIsIndependentFromInnerItemOrder() {
        val items = listOf(file("photo2.jpg"), file("photo10.jpg"),
            file("report2.txt"), file("report10.txt"))
        val options = FileArrangement(groupBy = GroupBy.TYPE,
            groupOrder = SortOrder.DESCENDING, sortOrder = SortOrder.DESCENDING)
        val groups = FileArrangementEngine.groups(items, options)
        assertEquals(listOf("Imágenes","Documentos"), groups.map { it.heading })
        assertEquals(listOf("photo10.jpg","photo2.jpg"), groups[0].files.map { it.name })
        assertEquals(items.size, groups.sumOf { it.files.size })
        assertEquals(items.size, groups.flatMap { it.files }.map { it.path }.distinct().size)
    }

    @Test fun sizeRangesAreNonOverlappingAtEveryBoundary() {
        val values = listOf(0L, 1023L, 1024L, 1_048_575L, 1_048_576L,
            1_073_741_823L, 1_073_741_824L)
        val expected = listOf("b0","b0","b1","b1","b2","b2","b3")
        val now = Instant.parse("2026-10-31T12:00:00Z")
        assertEquals(expected, values.map {
            FileArrangementEngine.groupKey(file("x", size = it), GroupBy.SIZE, now, ZoneId.of("UTC")).id
        })
        assertEquals("folder", FileArrangementEngine.groupKey(file("folder", directory = true),
            GroupBy.SIZE, now, ZoneId.of("UTC")).id)
        assertEquals("unknown", FileArrangementEngine.groupKey(file("unknown", size = -1),
            GroupBy.SIZE, now, ZoneId.of("UTC")).id)
    }

    @Test fun groupsInitialsAndUnknownDateWithoutUsingExactValues() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-10-31T12:00:00Z")
        val files = listOf(file("Árbol.txt"), file("Apple.txt"), file("2files"),
            file("_hidden"), file("none", modified = 0))
        val initials = FileArrangementEngine.groups(files, FileArrangement(groupBy = GroupBy.NAME),
            now, zone)
        assertEquals(4, initials.size)
        assertEquals(2, initials.first { it.heading == "A" }.files.size)
        assertEquals("unknown", FileArrangementEngine.groupKey(files.last(),
            GroupBy.DATE, now, zone).id)
    }

    @Test fun dateGroupsFollowChronologyInBothDirections() {
        val now = Instant.parse("2026-10-31T12:00:00Z")
        val zone = ZoneId.of("UTC")
        val items = listOf(
            file("old", modified = Instant.parse("2024-06-01T12:00:00Z").toEpochMilli()),
            file("yesterday", modified = Instant.parse("2026-10-30T12:00:00Z").toEpochMilli()),
            file("today", modified = now.toEpochMilli()),
        )
        val asc = FileArrangementEngine.groups(items, FileArrangement(groupBy = GroupBy.DATE), now, zone)
        assertEquals(listOf("2024", "Ayer", "Hoy"), asc.map { it.heading })
        val desc = FileArrangementEngine.groups(items, FileArrangement(groupBy = GroupBy.DATE,
            groupOrder = SortOrder.DESCENDING), now, zone)
        assertEquals(asc.map { it.key }.reversed(), desc.map { it.key })
    }

    @Test fun foldersComeFirstInsideEachGroupWithoutDuplicatingItems() {
        val files = listOf(file("alpha.txt"), file("alpha-folder", directory = true),
            file("beta.mp4"), file("beta-folder", directory = true))
        val groups = FileArrangementEngine.groups(files, FileArrangement(groupBy = GroupBy.NAME))
        assertEquals(files.size, groups.sumOf { it.files.size })
        assertTrue(groups.flatMap { it.files }.distinctBy { it.path }.size == files.size)
        assertTrue(groups.first { it.heading == "A" }.files.first().isDirectory)
    }
}
