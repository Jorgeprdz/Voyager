package com.voyagerfiles.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class FinderDragDropStateTest {
    @Test fun dropOnAnotherFolderProducesConfirmedMoveRequest() {
        val state = FinderDragDropState { true }.apply { enabled = true }
        state.register("folder", "/root/destination", Rect(0f, 0f, 100f, 100f))
        state.start(listOf("/root/source/file.txt"), Offset(25f, 25f))
        assertEquals("/root/destination", state.hoveredDestination)
        state.end()
        assertEquals(FinderPendingMove(listOf("/root/source/file.txt"), "/root/destination"), state.pending)
        state.dismiss()
        assertNull(state.pending)
    }

    @Test fun refusesSelfDescendantsAndSameParentDrops() {
        val state = FinderDragDropState { true }.apply { enabled = true }
        state.register("self", "/root/Folder", Rect(0f, 0f, 100f, 100f))
        state.start(listOf("/root/Folder"), Offset(25f, 25f))
        state.end()
        assertNull(state.pending)
        state.unregister("self")
        state.register("child", "/root/Folder/Child", Rect(0f, 0f, 100f, 100f))
        state.start(listOf("/root/Folder"), Offset(25f, 25f))
        state.end()
        assertNull(state.pending)
        state.unregister("child")
        state.register("parent", "/root", Rect(0f, 0f, 100f, 100f))
        state.start(listOf("/root/file.txt"), Offset(25f, 25f))
        state.end()
        assertNull(state.pending)
    }

    @Test fun emptyDropAreaDoesNotMoveAnything() {
        val state = FinderDragDropState { true }.apply { enabled = true }
        state.start(listOf("/root/source/file.txt"), Offset(100f, 100f))
        state.end()
        assertNull(state.pending)
    }
    @Test fun externalTargetUsesSmallestWritableFolder() {
        val state = FinderDragDropState { path -> !path.endsWith("restricted") }
        state.allowedRoot = "/root"
        state.register("parent", "/root/parent", Rect(0f, 0f, 100f, 100f))
        state.register("nested", "/root/parent/child", Rect(10f, 10f, 40f, 40f))
        state.register("restricted", "/root/restricted", Rect(60f, 60f, 90f, 90f))
        assertEquals("/root/parent/child", state.externalTargetAt(Offset(20f, 20f)))
        assertEquals("/root/parent", state.externalTargetAt(Offset(70f, 70f)))
        state.updateExternalHover(Offset(20f, 20f))
        assertEquals("/root/parent/child", state.externalHoveredDestination)
        state.clearExternalHover()
        assertNull(state.externalHoveredDestination)
        assertTrue(state.draggingPaths.isEmpty())
    }
    @Test fun panelRequestsRequireSafeWritableDestinationAndExplicitAction() {
        val state = FinderDragDropState { it != "/root/denied" }.apply { enabled = true; allowedRoot = "/root" }
        assertFalse(state.requestTransfer(listOf("/root/folder"), "/root/folder/child", FinderDropAction.MOVE))
        assertFalse(state.requestTransfer(listOf("/root/file"), "/outside", FinderDropAction.COPY))
        assertFalse(state.requestTransfer(listOf("/root/file"), "/root/denied", FinderDropAction.MOVE))
        assertTrue(state.requestTransfer(listOf("/root/file"), "/root/other", FinderDropAction.COPY))
        assertEquals(FinderDropAction.COPY, state.pending!!.action)
        assertFalse(state.awaitingTransfer)
    }
}
