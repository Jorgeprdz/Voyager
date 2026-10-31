package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderZoomPresetsTest {
    @Test fun iconsHaveFiveSizes() {
        assertEquals(listOf(64f,84f,104f,128f,160f),
            (0..4).map { FinderZoomPresets.density(FinderViewStyle.ICONS,it).iconDp })
    }
    @Test fun listUsesCompactRows() {
        assertEquals(listOf(24f,30f,37f,44f),
            (0..3).map { FinderZoomPresets.density(FinderViewStyle.LIST,it).rowHeightDp })
    }
    @Test fun viewsHaveSeparateScales() {
        assertNotEquals(FinderZoomPresets.density(FinderViewStyle.ICONS,3).iconDp,
            FinderZoomPresets.density(FinderViewStyle.COLUMNS,3).iconDp)
    }
    @Test fun indicesAreClamped() {
        assertEquals(0,FinderZoomPresets.clamp(FinderViewStyle.ICONS,-1))
        assertEquals(3,FinderZoomPresets.clamp(FinderViewStyle.LIST,10))
    }
}
