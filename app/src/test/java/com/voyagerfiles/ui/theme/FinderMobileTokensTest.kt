package com.voyagerfiles.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FinderMobileTokensTest {
    @Test fun bothModesUseFinderColorsNotMaterialDynamic() {
        val light = FinderMobileTokens.colorScheme(false)
        val dark = FinderMobileTokens.colorScheme(true)
        assertEquals(FinderMobileTokens.background, light.background)
        assertEquals(FinderMobileTokens.navy, light.onSurface)
        assertEquals(FinderMobileTokens.folderBlue, light.primary)
        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.onSurface, dark.onSurface)
        assertEquals(Color.Transparent, light.surfaceTint)
        assertEquals(Color.Transparent, dark.surfaceTint)
    }

    @Test fun touchTargetRemainsAccessible() {
        assertEquals(48.dp, FinderMobileTokens.touchTarget)
    }
}
