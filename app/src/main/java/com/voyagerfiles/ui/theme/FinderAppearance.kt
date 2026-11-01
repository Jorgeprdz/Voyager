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

/** Style and luminosity are independent. GLASS is the migration-safe default. */
data class FinderAppearance(
    val style: FinderStyle = FinderStyle.GLASS,
    val brightness: FinderBrightness = FinderBrightness.AUTOMATIC,
    val highContrast: Boolean = false,
    val reduceTransparency: Boolean = false,
)

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
)

val LocalFinderAppearance = staticCompositionLocalOf { FinderAppearance() }
val LocalFinderSemanticTokens = staticCompositionLocalOf {
    FinderAppearanceResolver.tokens(FinderAppearance(), false)
}

/** Common semantic layer: no changes to hit targets, navigation, file identity or interactions. */
object FinderAppearanceResolver {
    fun resolveDark(mode: FinderBrightness, systemDark: Boolean): Boolean = when (mode) {
        FinderBrightness.AUTOMATIC -> systemDark
        FinderBrightness.LIGHT -> false
        FinderBrightness.DARK -> true
    }

    fun tokens(value: FinderAppearance, dark: Boolean): FinderSemanticTokens {
        val contrast = value.highContrast
        val opaque = value.reduceTransparency || contrast
        val background = when (value.style) {
            FinderStyle.GLASS -> if (dark) Color(0xFF101A2A) else Color(0xFFF8FBFF)
            FinderStyle.MATERIAL -> if (dark) Color(0xFF15131B) else Color(0xFFFCF8FF)
            FinderStyle.ONE_UI -> if (dark) Color(0xFF0E1015) else Color(0xFFF7F8FB)
            FinderStyle.FLUENT -> if (dark) Color(0xFF181A1E) else Color(0xFFF4F5F7)
        }
        val surface = when (value.style) {
            FinderStyle.GLASS -> if (dark) Color(0xFF19263A) else Color.White
            FinderStyle.MATERIAL -> if (dark) Color(0xFF25222A) else Color(0xFFF3EDF7)
            FinderStyle.ONE_UI -> if (dark) Color(0xFF1C1F27) else Color.White
            FinderStyle.FLUENT -> if (dark) Color(0xFF24262C) else Color(0xFFFDFDFD)
        }
        val raised = when (value.style) {
            FinderStyle.GLASS -> if (dark) Color(0xFF23354C) else Color(0xFFEAF4FF)
            FinderStyle.MATERIAL -> if (dark) Color(0xFF36303B) else Color(0xFFE9E1EE)
            FinderStyle.ONE_UI -> if (dark) Color(0xFF292D39) else Color(0xFFEBF0F8)
            FinderStyle.FLUENT -> if (dark) Color(0xFF343841) else Color(0xFFE6EBF3)
        }
        val accent = when (value.style) {
            FinderStyle.GLASS -> if (dark) Color(0xFF8CC8FF) else Color(0xFF2777C6)
            FinderStyle.MATERIAL -> if (dark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
            FinderStyle.ONE_UI -> if (dark) Color(0xFF8AB6FF) else Color(0xFF286ADB)
            FinderStyle.FLUENT -> if (dark) Color(0xFF82C5FF) else Color(0xFF0067C0)
        }
        val text = if (dark) Color(0xFFF7FAFF) else Color(0xFF172238)
        val secondary = if (contrast) text else if (dark) Color(0xFFC9D5E2) else Color(0xFF4D5E70)
        val selected = if (contrast) accent else if (dark) raised else raised
        return FinderSemanticTokens(
            appearance = value, dark = dark, background = background, surface = surface,
            raised = raised, navigation = if (value.style == FinderStyle.FLUENT) surface else background,
            accent = accent, onSurface = text, secondaryText = secondary,
            border = if (contrast) accent else if (dark) Color(0xFF7F91A9) else Color(0xFFB4C2D2),
            selected = selected, onSelected = text, focus = accent,
            folder = if (value.style == FinderStyle.MATERIAL) accent else Color(0xFF2777C6),
            corner = when (value.style) {
                FinderStyle.GLASS -> 17
                FinderStyle.MATERIAL -> 22
                FinderStyle.ONE_UI -> 20
                FinderStyle.FLUENT -> 9
            },
            borderDp = if (contrast) 2f else 1f,
            elevationDp = when (value.style) {
                FinderStyle.GLASS -> 7f
                FinderStyle.MATERIAL -> 3f
                FinderStyle.ONE_UI -> 2f
                FinderStyle.FLUENT -> 4f
            },
            glassAlpha = if (opaque) 1f else if (value.style == FinderStyle.GLASS) .91f
                else if (value.style == FinderStyle.FLUENT) .95f else 1f,
        )
    }

    fun shapes(tokens: FinderSemanticTokens): Shapes = Shapes(
        extraSmall = RoundedCornerShape((tokens.corner - 10).coerceAtLeast(3).dp),
        small = RoundedCornerShape((tokens.corner - 5).coerceAtLeast(4).dp),
        medium = RoundedCornerShape(tokens.corner.dp),
        large = RoundedCornerShape((tokens.corner + 3).dp),
        extraLarge = RoundedCornerShape((tokens.corner + 7).dp),
    )

    fun scheme(tokens: FinderSemanticTokens, dynamic: ColorScheme? = null): ColorScheme {
        val t = tokens
        val colors = if (t.appearance.style == FinderStyle.MATERIAL && dynamic != null) dynamic
            else if (t.dark) darkColorScheme(
                primary = t.accent, onPrimary = t.background, primaryContainer = t.raised,
                onPrimaryContainer = t.onSurface, secondary = t.accent, onSecondary = t.background,
                secondaryContainer = t.raised, onSecondaryContainer = t.onSurface,
                background = t.background, onBackground = t.onSurface,
                surface = t.surface, onSurface = t.onSurface,
                surfaceVariant = t.raised, onSurfaceVariant = t.secondaryText,
                outline = t.border, outlineVariant = t.border, surfaceTint = t.accent,
            ) else lightColorScheme(
                primary = t.accent, onPrimary = Color.White, primaryContainer = t.raised,
                onPrimaryContainer = t.onSurface, secondary = t.accent, onSecondary = Color.White,
                secondaryContainer = t.raised, onSecondaryContainer = t.onSurface,
                background = t.background, onBackground = t.onSurface,
                surface = t.surface, onSurface = t.onSurface,
                surfaceVariant = t.raised, onSurfaceVariant = t.secondaryText,
                outline = t.border, outlineVariant = t.border, surfaceTint = t.accent,
            )
        return if (!t.appearance.highContrast) colors else colors.copy(
            onBackground = if (t.dark) Color.White else Color.Black,
            onSurface = if (t.dark) Color.White else Color.Black,
            onSurfaceVariant = if (t.dark) Color.White else Color.Black,
            outline = t.accent, outlineVariant = t.accent,
        )
    }
}
