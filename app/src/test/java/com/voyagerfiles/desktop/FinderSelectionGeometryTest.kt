package com.voyagerfiles.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.SortBy
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class FinderSelectionGeometryTest {
    @Test fun marqueeOnlyIncludesIntersectingItems() {
        val bounds = mapOf(
            "/a" to Rect(0f, 0f, 40f, 40f),
            "/b" to Rect(55f, 0f, 95f, 40f),
            "/c" to Rect(0f, 60f, 40f, 100f),
        )
        assertEquals(setOf("/a", "/b"), finderPathsInRect(bounds, Rect(25f, 10f, 70f, 30f)))
        assertTrue(finderPathsInRect(bounds, Rect(120f, 120f, 160f, 160f)).isEmpty())
    }

    @Test fun marqueeUsesActualBoundsAndSupportsOneHundredItems() {
        val marquee = FinderMarqueeState()
        repeat(100) { i ->
            marquee.register("/file-$i", Rect((i % 10) * 50f, (i / 10) * 50f,
                (i % 10) * 50f + 42f, (i / 10) * 50f + 42f))
        }
        assertTrue(marquee.startDrag(Offset(-10f, -10f), emptySet(), false))
        assertEquals(100, marquee.update(Offset(500f, 500f)).size)
        marquee.finish()
        assertFalse(marquee.active)
    }

    @Test fun edgeAutoscrollOnlyStartsNearViewportBounds() {
        val viewport = Rect(0f, 0f, 300f, 400f)
        assertTrue(finderVerticalEdgeSpeed(Offset(140f, 5f), viewport) < 0f)
        assertTrue(finderVerticalEdgeSpeed(Offset(140f, 395f), viewport) > 0f)
        assertEquals(0f, finderVerticalEdgeSpeed(Offset(140f, 200f), viewport))
        assertEquals(0f, finderVerticalEdgeSpeed(Offset(400f, 395f), viewport))
    }

    @Test fun orderingGroupsFoldersButDoesNotChangeIds() {
        val files = listOf(
            FileItem("b.txt", "/b.txt", false, 90, Date(20)),
            FileItem("z", "/z", true, 0, Date(10)),
            FileItem("a.txt", "/a.txt", false, 1, Date(30)),
        )
        val ordered = finderOrderFiles(files, SortBy.SIZE, true)
        assertEquals(listOf("/z", "/b.txt", "/a.txt"), ordered.map { it.path })
        assertEquals(files.map { it.path }.toSet(), ordered.map { it.path }.toSet())
    }
}
