package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderGlassMaterialsTest {
    @Test fun lightAndDarkAreSeparatePalettes() {
        assertFalse(FinderGlassPalette.Light.dark)
        assertTrue(FinderGlassPalette.Dark.dark)
        assertNotEquals(FinderGlassPalette.Light.content, FinderGlassPalette.Dark.content)
        assertNotEquals(FinderGlassPalette.Light.sidebar, FinderGlassPalette.Dark.sidebar)
    }

    @Test fun selectedItemRemainsVisibleAfterWindowLosesFocus() {
        listOf(FinderGlassPalette.Light, FinderGlassPalette.Dark).forEach { palette ->
            assertNotEquals(palette.content, palette.selection(true, false))
            assertNotEquals(palette.selectedStrong, palette.selectedInactive)
            assertNotEquals(palette.selectionText(true, true), palette.selectedInactive)
        }
    }

    @Test fun reducedMotionDisablesMaterialTransitions() {
        val p = FinderGlassPalette.Light
        assertEquals(0, FinderMaterials(p, true, true, false).hoverMillis)
        assertEquals(0, FinderMaterials(p, true, true, false).selectionMillis)
        assertTrue(FinderMaterials(p, true, false, false).surfaceMillis in 150..220)
    }
}
