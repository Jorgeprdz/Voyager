package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderContentTokensTest {
    @Test fun listKeepsSizeAndDateOnlyWhenUseful() {
        assertEquals(FinderListColumns(false, false), FinderContentTokens.listColumns(430f))
        assertEquals(FinderListColumns(true, false), FinderContentTokens.listColumns(530f))
        assertEquals(FinderListColumns(true, true), FinderContentTokens.listColumns(920f))
    }

    @Test fun heroFitsSmallPanelsWithoutGrowingBeyondLargeDisplayLimit() {
        assertEquals(84f, FinderContentTokens.galleryHeroSize(140f, 140f), 0.001f)
        assertTrue(FinderContentTokens.galleryHeroSize(310f, 330f) < 260f)
        assertEquals(260f, FinderContentTokens.galleryHeroSize(960f, 800f), 0.001f)
    }

    @Test fun formatLabelsArePureAndCaseInsensitive() {
        assertEquals("PDF", FinderContentTokens.fileFormat(".PDF"))
        assertEquals("IMG", FinderContentTokens.fileFormat("heic"))
        assertEquals("VIDEO", FinderContentTokens.fileFormat("MP4"))
        assertEquals("ZIP", FinderContentTokens.fileFormat("7z"))
        assertNull(FinderContentTokens.fileFormat("unknown"))
    }

    @Test fun folderPaletteIsDistinctInLightAndDark() {
        assertNotEquals(FinderFolderColors.Light.base, FinderFolderColors.Dark.base)
        assertNotEquals(FinderFolderColors.Light.bottom, FinderFolderColors.Dark.bottom)
    }
}
