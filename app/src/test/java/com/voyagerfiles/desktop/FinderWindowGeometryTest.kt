package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

/** Pure geometry contracts; static review only until the final build phase. */
class FinderWindowGeometryTest {
    @Test fun roomyDesktopUsesPreferredSidebarAndPreview() {
        val shell = FinderWindowGeometry.resolve(1440f, 900f, 250f, 190f)
        assertEquals(250f, shell.sidebarWidthDp, 0.001f)
        assertFalse(shell.sidebarRail)
        assertFalse(shell.compactToolbar)
        assertTrue(shell.showPreview)
        assertEquals(28f, shell.cornerRadiusDp, 0.001f)
    }

    @Test fun resizeToMediumKeepsContentAndClampsSidebar() {
        val shell = FinderWindowGeometry.resolve(850f, 620f, 400f, 180f)
        assertEquals(400f, shell.sidebarWidthDp, 0.001f)
        assertFalse(shell.sidebarRail)
        assertTrue(shell.compactToolbar)
        assertFalse(shell.showPreview)
        assertTrue(850f - shell.sidebarWidthDp - FinderWindowGeometry.DIVIDER_HIT_WIDTH_DP >=
            FinderWindowGeometry.CONTENT_MIN_WIDTH_DP)
    }

    @Test fun narrowWindowUsesFunctionalRailInsteadOfWideSidebar() {
        val shell = FinderWindowGeometry.resolve(540f, 540f, 260f, 170f)
        assertEquals(64f, shell.sidebarWidthDp, 0.001f)
        assertTrue(shell.sidebarRail)
        assertTrue(shell.compactToolbar)
        assertTrue(shell.minimalToolbar)
        assertFalse(shell.showPreview)
        assertEquals(24f, shell.cornerRadiusDp, 0.001f)
    }

    @Test fun verySmallWindowNeverAllocatesNegativeSpace() {
        val shell = FinderWindowGeometry.resolve(170f, 180f, 250f, 180f)
        assertEquals(0f, shell.sidebarWidthDp, 0.001f)
        assertTrue(shell.sidebarRail)
        assertTrue(shell.minimalToolbar)
        assertEquals(26f, shell.statusBarHeightDp, 0.001f)
    }

    @Test fun sidebarRespectsNewLimitsAndPreservesMainContent() {
        val wide = FinderWindowGeometry.resolve(1600f,900f,500f)
        assertEquals(420f,wide.sidebarWidthDp,0.001f)
        val minimum = FinderWindowGeometry.resolve(950f,800f,130f)
        assertEquals(190f,minimum.sidebarWidthDp,0.001f)
        val tight = FinderWindowGeometry.resolve(730f,700f,420f)
        assertTrue(730f - tight.sidebarWidthDp - FinderWindowGeometry.DIVIDER_HIT_WIDTH_DP >=
            FinderWindowGeometry.CONTENT_MIN_WIDTH_DP)
        assertEquals(64f,FinderWindowGeometry.resolve(690f,800f,420f).sidebarWidthDp,0.001f)
        assertEquals(250f,FinderWindowGeometry.resolve(1400f,800f,250f).sidebarWidthDp,0.001f)
    }

    @Test fun preferredSidebarIsPreservedAcrossReasonableDesktopResizes() {
        val medium = FinderWindowGeometry.resolve(980f, 680f, 265f, 180f)
        val maximized = FinderWindowGeometry.resolve(1600f, 900f, 265f, 180f)
        assertEquals(265f, medium.sidebarWidthDp, 0.001f)
        assertEquals(265f, maximized.sidebarWidthDp, 0.001f)
        assertFalse(medium.sidebarRail)
        assertFalse(maximized.sidebarRail)
        assertFalse(medium.showPreview)
        assertTrue(maximized.showPreview)
    }
}
