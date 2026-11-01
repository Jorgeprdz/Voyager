package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderSidebarGlyphTest {
    @Test fun standardFoldersHaveSemanticVectors() {
        assertEquals("download",finderSidebarGlyph("Download"))
        assertEquals("document",finderSidebarGlyph("Documents"))
        assertEquals("picture",finderSidebarGlyph("Pictures"))
        assertEquals("music",finderSidebarGlyph("Music"))
        assertEquals("video",finderSidebarGlyph("Movies"))
    }
    @Test fun userFavoritesKeepFolderFallbackOrCustomImages() {
        assertEquals("folder-outline",finderSidebarGlyph("My project"))
    }
}
