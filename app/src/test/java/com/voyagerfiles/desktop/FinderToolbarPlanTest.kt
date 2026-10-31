package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderToolbarPlanTest {
    @Test fun wideShowsAllDirectGroups() {
        val p = FinderToolbarPlan.resolve(1040f, false)
        assertEquals(FinderToolbarMode.WIDE, p.mode)
        assertTrue(p.showForward)
        assertTrue(p.fullViewSelector)
        assertTrue(p.directFileActions)
        assertTrue(p.directAdvancedActions)
        assertFalse(p.overflow)
    }
    @Test fun mediumRetainsFourViewsAndMovesSecondaryActionsToOverflow() {
        val p = FinderToolbarPlan.resolve(730f, false)
        assertEquals(FinderToolbarMode.MEDIUM, p.mode)
        assertTrue(p.fullViewSelector)
        assertTrue(p.directFileActions)
        assertFalse(p.directAdvancedActions)
        assertTrue(p.overflow)
    }
    @Test fun compactKeepsPrimaryControlsAndOverflow() {
        val p = FinderToolbarPlan.resolve(410f, true)
        assertEquals(FinderToolbarMode.COMPACT, p.mode)
        assertTrue(p.showTitle)
        assertTrue(p.showForward)
        assertFalse(p.fullViewSelector)
        assertFalse(p.directFileActions)
        assertFalse(p.directAdvancedActions)
        assertFalse(p.inlineSearch)
        assertTrue(p.overflow)
    }
    @Test fun tightPreservesForwardAndFullPathThroughOverflow() {
        val p = FinderToolbarPlan.resolve(150f, true)
        assertEquals(FinderToolbarMode.TIGHT, p.mode)
        assertFalse(p.showTitle)
        assertFalse(p.showForward)
        assertTrue(p.overflow)
        assertFalse(p.inlineSearch)
    }
    @Test fun inlineSearchRequiresExtraWidth() {
        assertFalse(FinderToolbarPlan.resolve(950f, true).inlineSearch)
        assertTrue(FinderToolbarPlan.resolve(1150f, true).inlineSearch)
        assertFalse(FinderToolbarPlan.resolve(1150f, false).inlineSearch)
    }
    @Test fun negativeWidthIsSafe() {
        assertEquals(FinderToolbarMode.TIGHT, FinderToolbarPlan.resolve(-20f, false).mode)
    }
}
