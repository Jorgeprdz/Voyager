package com.voyagerfiles.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class FinderThemeVisualContractTest {
    private fun contrast(a: Color, b: Color): Float {
        val high = maxOf(a.luminance(), b.luminance())
        val low = minOf(a.luminance(), b.luminance())
        return (high + .05f) / (low + .05f)
    }

    @Test fun documentedWindowAndAccentHexValuesForEightPalettes() {
        data class Case(val style: FinderStyle, val dark: Boolean, val window: Long, val accent: Long)
        listOf(
            Case(FinderStyle.GLASS, false, 0xFFF3F4F7, 0xFF0066CC),
            Case(FinderStyle.GLASS, true, 0xFF202126, 0xFF8FC1FF),
            Case(FinderStyle.MATERIAL, false, 0xFFF8F9FF, 0xFF285EA8),
            Case(FinderStyle.MATERIAL, true, 0xFF111318, 0xFFAAC7FF),
            Case(FinderStyle.ONE_UI, false, 0xFFF2F3F5, 0xFF1457D9),
            Case(FinderStyle.ONE_UI, true, 0xFF101113, 0xFF91B7FF),
            Case(FinderStyle.FLUENT, false, 0xFFF3F3F3, 0xFF005A9E),
            Case(FinderStyle.FLUENT, true, 0xFF202020, 0xFF80CAFF),
        ).forEach { c ->
            val t = FinderAppearanceResolver.tokens(FinderAppearance(style = c.style), c.dark)
            assertEquals(c.style.name, Color(c.window.toInt()), t.background)
            assertEquals(c.style.name, Color(c.accent.toInt()), t.accent)
        }
    }

    @Test fun normalSecondarySelectedAndAccentTextExceedFourPointFive() {
        FinderStyle.entries.forEach { style ->
            listOf(false, true).forEach { dark ->
                val t = FinderAppearanceResolver.tokens(FinderAppearance(style = style), dark)
                for ((text, surface) in listOf(
                    t.onSurface to t.surface,
                    t.secondaryText to t.surface,
                    t.onSelected to t.selected,
                    t.onAccent to t.accent,
                )) {
                    assertTrue("$style dark=$dark contrast=${contrast(text, surface)}",
                        contrast(text, surface) >= 4.5f)
                }
            }
        }
    }

    @Test fun dynamicMaterialIsAllOrNothingAndOptional() {
        val custom = darkColorScheme(primary = Color.Magenta)
        val enabled = FinderAppearanceResolver.tokens(FinderAppearance(style = FinderStyle.MATERIAL), true)
        val disabled = FinderAppearanceResolver.tokens(FinderAppearance(
            style = FinderStyle.MATERIAL, dynamicColors = false), true)
        assertEquals(custom, FinderAppearanceResolver.scheme(enabled, custom))
        assertNotEquals(custom, FinderAppearanceResolver.scheme(disabled, custom))
    }

    @Test fun fluentFolderIsYellowAndMaterialFolderIsFlat() {
        val fluent = FinderAppearanceResolver.tokens(FinderAppearance(style = FinderStyle.FLUENT), false)
        assertEquals(Color(0xFFD99A19), fluent.folder)
        assertEquals(Color(0xFFF3C75F), fluent.folderFront)
        val material = FinderAppearanceResolver.tokens(FinderAppearance(style = FinderStyle.MATERIAL), false)
        assertEquals(Color(0xFF285EA8), material.folder)
    }

    @Test fun themeGeometryTokensVaryButHitTargetsAreNotPartOfTheme() {
        val values = FinderStyle.entries.map { style ->
            FinderAppearanceResolver.tokens(FinderAppearance(style = style), false)
        }
        assertEquals(listOf(8, 16, 14, 4), values.map { it.buttonRadiusDp })
        assertEquals(listOf(12, 16, 18, 8), values.map { it.menuRadiusDp })
        assertEquals(listOf(20, 28, 24, 12), values.map { it.dialogRadiusDp })
    }
}
