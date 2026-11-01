package com.voyagerfiles.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class FinderStyle { GLASS, MATERIAL, ONE_UI, FLUENT }
enum class FinderBrightness { AUTOMATIC, LIGHT, DARK }

/** Existing installations keep GLASS and their legacy custom color scheme by default. */
data class FinderAppearance(
    val style: FinderStyle = FinderStyle.GLASS,
    val brightness: FinderBrightness = FinderBrightness.AUTOMATIC,
    val highContrast: Boolean = false,
    val reduceTransparency: Boolean = false,
    val dynamicColors: Boolean = true,
)

/**
 * The same named roles are used by Compose Material, the DeX palette, thumbnails and
 * previews. All HEX values are FinderDex's own design tokens, not OEM brand artwork.
 */
data class FinderSemanticTokens(
    val appearance: FinderAppearance,
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val raised: Color,
    val navigation: Color,
    val accent: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val border: Color,
    val selected: Color,
    val onSelected: Color,
    val focus: Color,
    val folder: Color,
    val corner: Int,
    val borderDp: Float,
    val elevationDp: Float,
    val glassAlpha: Float,
    val onAccent: Color = Color.White,
    val selectedInactive: Color = Color.LightGray,
    val folderFront: Color = Color(0xFF4E91D5),
    val buttonRadiusDp: Int = 8,
    val selectionRadiusDp: Int = 8,
    val menuRadiusDp: Int = 12,
    val dialogRadiusDp: Int = 20,
)

val LocalFinderAppearance = staticCompositionLocalOf { FinderAppearance() }
val LocalFinderSemanticTokens = staticCompositionLocalOf {
    FinderAppearanceResolver.tokens(FinderAppearance(), false)
}

object FinderAppearanceResolver {
    fun resolveDark(mode: FinderBrightness, systemDark: Boolean): Boolean = when (mode) {
        FinderBrightness.AUTOMATIC -> systemDark
        FinderBrightness.LIGHT -> false
        FinderBrightness.DARK -> true
    }

    fun tokens(value: FinderAppearance, dark: Boolean): FinderSemanticTokens {
        // window, content, sidebar, raised, ink, secondary, divider, accent, onAccent,
        // selection, onSelection, inactive, folder back, folder front
        val c: LongArray = when (value.style) {
            FinderStyle.GLASS -> if (dark) longArrayOf(
                0xFF202126, 0xFF18191D, 0xFF262830, 0xFF30323B, 0xFFF3F4F7,
                0xFFB1B6C2, 0xFF454A56, 0xFF8FC1FF, 0xFF102B49,
                0xFF28486B, 0xFFF3F7FF, 0xFF373B45, 0xFF2776C7, 0xFF559EDD)
            else longArrayOf(
                0xFFF3F4F7, 0xFFFFFFFF, 0xFFEDEFF4, 0xFFFAFBFD, 0xFF202124,
                0xFF5F636B, 0xFFD9DDE5, 0xFF0066CC, 0xFFFFFFFF,
                0xFFD9EAFE, 0xFF123E70, 0xFFE7E9EE, 0xFF2776C7, 0xFF559EDD)
            FinderStyle.MATERIAL -> if (dark) longArrayOf(
                0xFF111318, 0xFF111318, 0xFF191C22, 0xFF252932, 0xFFE2E2E9,
                0xFFC3C6D0, 0xFF8D919A, 0xFFAAC7FF, 0xFF0A305F,
                0xFF284777, 0xFFD6E3FF, 0xFF252932, 0xFF789CCA, 0xFFAAC7FF)
            else longArrayOf(
                0xFFF8F9FF, 0xFFFEFBFF, 0xFFEDF0FA, 0xFFE7EAF4, 0xFF191C20,
                0xFF44474F, 0xFF74777F, 0xFF285EA8, 0xFFFFFFFF,
                0xFFD6E3FF, 0xFF102F60, 0xFFEDF0FA, 0xFF285EA8, 0xFF85A7DA)
            FinderStyle.ONE_UI -> if (dark) longArrayOf(
                0xFF101113, 0xFF17181B, 0xFF131417, 0xFF222429, 0xFFF3F4F6,
                0xFFAFB5BF, 0xFF3D4149, 0xFF91B7FF, 0xFF102E62,
                0xFF233C65, 0xFFE5EEFF, 0xFF30343C, 0xFF447CCF, 0xFF649AE6)
            else longArrayOf(
                0xFFF2F3F5, 0xFFFFFFFF, 0xFFF7F8FA, 0xFFFFFFFF, 0xFF17181A,
                0xFF5B6069, 0xFFD8DBE0, 0xFF1457D9, 0xFFFFFFFF,
                0xFFE2ECFF, 0xFF123F94, 0xFFE8EAEE, 0xFF397EE4, 0xFF8AB5F8)
            FinderStyle.FLUENT -> if (dark) longArrayOf(
                0xFF202020, 0xFF191919, 0xFF252525, 0xFF2D2D2D, 0xFFF4F4F4,
                0xFFBDBDBD, 0xFF484848, 0xFF80CAFF, 0xFF123347,
                0xFF203F53, 0xFFE6F4FF, 0xFF343434, 0xFFB27915, 0xFFD7AA41)
            else longArrayOf(
                0xFFF3F3F3, 0xFFFFFFFF, 0xFFF0F0F0, 0xFFFCFCFC, 0xFF202020,
                0xFF5D5D5D, 0xFFD6D6D6, 0xFF005A9E, 0xFFFFFFFF,
                0xFFDCECF9, 0xFF173A56, 0xFFE7E7E7, 0xFFD99A19, 0xFFF3C75F)
        }
        val contrast = value.highContrast
        val solid = value.reduceTransparency || contrast
        fun color(index: Int) = Color(c[index].toInt())
        val radii = when (value.style) {
            FinderStyle.GLASS -> listOf(8, 8, 12, 20)
            FinderStyle.MATERIAL -> listOf(16, 12, 16, 28)
            FinderStyle.ONE_UI -> listOf(14, 12, 18, 24)
            FinderStyle.FLUENT -> listOf(4, 4, 8, 12)
        }
        val ink = if (!contrast) color(4) else if (dark) Color.White else Color.Black
        return FinderSemanticTokens(
            appearance = value, dark = dark, background = color(0), surface = color(1),
            raised = color(3), navigation = color(2), accent = color(7),
            onSurface = ink, secondaryText = if (contrast) ink else color(5),
            border = if (contrast) color(7) else color(6),
            selected = color(9), onSelected = if (contrast) ink else color(10),
            focus = color(7), folder = color(12), folderFront = color(13),
            onAccent = color(8), selectedInactive = color(11),
            corner = radii[2], borderDp = if (contrast) 2f else 1f,
            elevationDp = if (value.style == FinderStyle.GLASS) 7f
                else if (value.style == FinderStyle.FLUENT) 4f else 2f,
            glassAlpha = if (solid) 1f else when (value.style) {
                FinderStyle.GLASS -> .92f
                FinderStyle.FLUENT -> .95f
                else -> 1f
            },
            buttonRadiusDp = radii[0], selectionRadiusDp = radii[1],
            menuRadiusDp = radii[2], dialogRadiusDp = radii[3],
        )
    }

    fun shapes(tokens: FinderSemanticTokens) = Shapes(
        extraSmall = RoundedCornerShape(tokens.selectionRadiusDp.dp),
        small = RoundedCornerShape(tokens.buttonRadiusDp.dp),
        medium = RoundedCornerShape(tokens.buttonRadiusDp.dp),
        large = RoundedCornerShape(tokens.menuRadiusDp.dp),
        extraLarge = RoundedCornerShape(tokens.dialogRadiusDp.dp),
    )

    fun scheme(t: FinderSemanticTokens, dynamic: ColorScheme? = null): ColorScheme {
        // Android Material adopts the COMPLETE system scheme, not mixed fallback roles.
        if (t.appearance.style == FinderStyle.MATERIAL && t.appearance.dynamicColors &&
            dynamic != null && !t.appearance.highContrast) return dynamic
        val colors = if (t.dark) darkColorScheme(
            primary = t.accent, onPrimary = t.onAccent,
            primaryContainer = t.selected, onPrimaryContainer = t.onSelected,
            secondary = t.accent, onSecondary = t.onAccent,
            secondaryContainer = t.selected, onSecondaryContainer = t.onSelected,
            background = t.background, onBackground = t.onSurface,
            surface = t.surface, onSurface = t.onSurface,
            surfaceVariant = t.raised, onSurfaceVariant = t.secondaryText,
            outline = t.border, outlineVariant = t.border,
            surfaceTint = t.accent, error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        ) else lightColorScheme(
            primary = t.accent, onPrimary = t.onAccent,
            primaryContainer = t.selected, onPrimaryContainer = t.onSelected,
            secondary = t.accent, onSecondary = t.onAccent,
            secondaryContainer = t.selected, onSecondaryContainer = t.onSelected,
            background = t.background, onBackground = t.onSurface,
            surface = t.surface, onSurface = t.onSurface,
            surfaceVariant = t.raised, onSurfaceVariant = t.secondaryText,
            outline = t.border, outlineVariant = t.border,
            surfaceTint = t.accent, error = Color(0xFFB3261E), onError = Color.White,
        )
        return colors.copy(
            surfaceContainerLowest = t.surface,
            surfaceContainerLow = t.surface,
            surfaceContainer = t.raised,
            surfaceContainerHigh = t.raised,
            surfaceContainerHighest = t.raised,
        )
    }
}
