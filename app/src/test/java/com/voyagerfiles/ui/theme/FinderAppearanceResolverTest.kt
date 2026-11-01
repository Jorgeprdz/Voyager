package com.voyagerfiles.ui.theme

import org.junit.Assert.*
import org.junit.Test

class FinderAppearanceResolverTest {
    @Test fun allStylesHaveSeparateDarkAndLightSemanticTokens() {
        FinderStyle.entries.forEach { style ->
            val input = FinderAppearance(style = style)
            val light = FinderAppearanceResolver.tokens(input, false)
            val dark = FinderAppearanceResolver.tokens(input, true)
            assertNotEquals(light.background, dark.background)
            assertNotEquals(light.onSurface, dark.onSurface)
            assertNotEquals(light.accent, dark.accent)
            assertTrue(light.corner >= 4)
            assertTrue(dark.corner >= 4)
            assertEquals(style, light.appearance.style)
        }
    }

    @Test fun automaticLightDarkFollowsSystemOnlyWhenRequested() {
        assertTrue(FinderAppearanceResolver.resolveDark(FinderBrightness.AUTOMATIC, true))
        assertFalse(FinderAppearanceResolver.resolveDark(FinderBrightness.AUTOMATIC, false))
        assertFalse(FinderAppearanceResolver.resolveDark(FinderBrightness.LIGHT, true))
        assertTrue(FinderAppearanceResolver.resolveDark(FinderBrightness.DARK, false))
    }

    @Test fun reduceTransparencyForcesOpaqueMaterialsForEveryStyleAndBrightness() {
        FinderStyle.entries.forEach { style ->
            listOf(true, false).forEach { dark ->
                val options = FinderAppearance(style = style, reduceTransparency = true)
                assertEquals(1f, FinderAppearanceResolver.tokens(options, dark).glassAlpha)
            }
        }
    }

    @Test fun highContrastUpgradesSemanticBordersWithoutDependingOnTransparency() {
        FinderStyle.entries.forEach { style ->
            for (dark in listOf(true, false)) {
                val tokens = FinderAppearanceResolver.tokens(
                    FinderAppearance(style = style, highContrast = true), dark)
                assertEquals(2f, tokens.borderDp)
                assertEquals(tokens.accent, tokens.focus)
                assertEquals(1f, tokens.glassAlpha)
            }
        }
    }

    @Test fun styleSelectionDoesNotChangeBrightnessOrAccessibilityPreferences() {
        val original = FinderAppearance(brightness = FinderBrightness.DARK,
            highContrast = true, reduceTransparency = true)
        val updated = original.copy(style = FinderStyle.FLUENT)
        assertEquals(FinderBrightness.DARK, updated.brightness)
        assertTrue(updated.highContrast)
        assertTrue(updated.reduceTransparency)
    }
}
