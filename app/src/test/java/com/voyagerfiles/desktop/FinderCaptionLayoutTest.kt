package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderCaptionLayoutTest {
    @Test fun integratedToolbarLeavesBothSystemControlGroupsUntouched() {
        val layout = finderCaptionLayout(1200, 40, listOf(CaptionBounds(0, 80), CaptionBounds(1000, 1200)))
        assertTrue(layout.integrated)
        assertEquals(80, layout.leading)
        assertEquals(200, layout.trailing)
        assertEquals(0, layout.topInset)
    }
    @Test fun resizedWindowRecomputesSafeSpace() {
        val layout = finderCaptionLayout(720, 40, listOf(CaptionBounds(0, 80), CaptionBounds(520, 720)))
        assertEquals(80, layout.leading)
        assertEquals(200, layout.trailing)
    }
    @Test fun unknownCaptionBoundsKeepToolbarBelowSystemControls() {
        val layout = finderCaptionLayout(720, 40, emptyList())
        assertFalse(layout.integrated)
        assertEquals(40, layout.topInset)
    }
    @Test fun fullscreenHasNoReservedCaptionSpace() {
        val layout = finderCaptionLayout(1200, 0, emptyList())
        assertEquals(0, layout.leading + layout.trailing + layout.topInset)
    }
}
