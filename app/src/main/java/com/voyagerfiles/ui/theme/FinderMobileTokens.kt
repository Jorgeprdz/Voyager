package com.voyagerfiles.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** FinderDex phone palette; independent from wallpaper dynamic colors and Desktop R2.2. */
object FinderMobileTokens {
    val background = Color(0xFFF8FBFF)
    val surface = Color(0xFFFFFFFF)
    val ice = Color(0xFFEAF4FF)
    val folderBlue = Color(0xFF2777C6)
    val navy = Color(0xFF203B63)
    val secondaryInk = Color(0xFF58718F)
    val edge = Color(0xFFD9E8FA)
    val cardShape = RoundedCornerShape(17.dp)
    val touchTarget = 48.dp
    val controlShape = RoundedCornerShape(16.dp)
    val menuShape = RoundedCornerShape(18.dp)
    val selectedShape = RoundedCornerShape(12.dp)
    val spacingSmall = 8.dp
    val spacingMedium = 12.dp
    val spacingLarge = 16.dp

    fun colorScheme(dark: Boolean): ColorScheme = if (dark) {
        val canvas = Color(0xFF101A2A)
        val panel = Color(0xFF19263A)
        val raised = Color(0xFF23354C)
        val ink = Color(0xFFEAF3FF)
        val muted = Color(0xFFB5C9E1)
        darkColorScheme(
            primary = Color(0xFF8CC8FF), onPrimary = Color(0xFF0C2746),
            primaryContainer = Color(0xFF294E76), onPrimaryContainer = ink,
            secondary = muted, onSecondary = canvas,
            secondaryContainer = raised, onSecondaryContainer = ink,
            tertiary = Color(0xFF9BD6FD), onTertiary = canvas,
            background = canvas, onBackground = ink,
            surface = panel, onSurface = ink,
            surfaceVariant = raised, onSurfaceVariant = muted,
            outline = Color(0xFF68819C), outlineVariant = Color(0xFF40546D),
            error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = ink, inverseOnSurface = canvas, inversePrimary = navy,
            surfaceTint = Color.Transparent,
        ).copy(
            surfaceDim = canvas, surfaceBright = raised,
            surfaceContainerLowest = canvas, surfaceContainerLow = panel,
            surfaceContainer = panel, surfaceContainerHigh = raised,
            surfaceContainerHighest = Color(0xFF30445D),
        )
    } else {
        lightColorScheme(
            primary = folderBlue, onPrimary = Color.White,
            primaryContainer = ice, onPrimaryContainer = navy,
            secondary = navy, onSecondary = Color.White,
            secondaryContainer = ice, onSecondaryContainer = navy,
            tertiary = Color(0xFF1767A7), onTertiary = Color.White,
            background = background, onBackground = navy,
            surface = surface, onSurface = navy,
            surfaceVariant = ice, onSurfaceVariant = secondaryInk,
            outline = Color(0xFFA1B8D2), outlineVariant = edge,
            error = Color(0xFFB3261E), onError = Color.White,
            errorContainer = Color(0xFFFFDAD4), onErrorContainer = Color(0xFF410002),
            inverseSurface = navy, inverseOnSurface = Color.White,
            inversePrimary = Color(0xFFA1D2FF), surfaceTint = Color.Transparent,
        ).copy(
            surfaceDim = Color(0xFFF0F5FA), surfaceBright = surface,
            surfaceContainerLowest = surface, surfaceContainerLow = background,
            surfaceContainer = ice, surfaceContainerHigh = Color(0xFFE2EFFC),
            surfaceContainerHighest = Color(0xFFD8E8F8),
        )
    }

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(14.dp),
        small = RoundedCornerShape(12.dp),
        medium = controlShape,
        large = menuShape,
        extraLarge = RoundedCornerShape(24.dp),
    )
}

/** Apply only inside AppNavigation's mobile branch; Desktop remains on VoyagerTheme. */
@Composable
fun FinderMobileTheme(content: @Composable () -> Unit) {
    val appearance = LocalFinderAppearance.current
    val tokens = LocalFinderSemanticTokens.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Legacy Glass retains its original mobile palette. Non-Glass uses the shared
    // app scheme so sheets, dialogs and browser chrome resolve the same roles.
    val defaultGlass = appearance == FinderAppearance()
    MaterialTheme(
        colorScheme = if (defaultGlass) FinderMobileTokens.colorScheme(dark)
            else MaterialTheme.colorScheme,
        typography = MaterialTheme.typography,
        shapes = if (defaultGlass) FinderMobileTokens.shapes else FinderAppearanceResolver.shapes(tokens),
        content = content,
    )
}
