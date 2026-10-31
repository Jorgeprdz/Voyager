package com.voyagerfiles.desktop

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Desktop-only material palette. Alpha represents glass-like layering *inside* the app,
 * not a claim that Samsung's desktop is blurred behind an ordinary Activity.
 */
internal data class FinderGlassPalette(
    val window: Color,
    val sidebar: Color,
    val titlebar: Color,
    val toolbar: Color,
    val content: Color,
    val footer: Color,
    val ink: Color,
    val secondaryInk: Color,
    val accent: Color,
    val icon: Color,
    val separator: Color,
    val edge: Color,
    val hover: Color,
    val pressed: Color,
    val selected: Color,
    val selectedStrong: Color,
    val selectedInactive: Color,
    val selectedText: Color,
    val selectedStrongText: Color,
    val focusRing: Color,
    val scrollbar: Color,
    val scrollbarHover: Color,
    val illuminatedEdge: Color,
    val feedback: Color,
    val progress: Color,
    val dragHint: Color,
    val dark: Boolean,
) {
    companion object {
        val Light = FinderGlassPalette(
            window = Color(0xfff6f8fc),
            sidebar = Color(0xdfe8f0fa),
            titlebar = Color(0xeaeaf2fc),
            toolbar = Color(0xeaf1f5fc),
            content = Color(0xfffdfefe),
            footer = Color(0xfff8fafe),
            ink = Color(0xff253348),
            secondaryInk = Color(0xff65758c),
            accent = Color(0xff1975d2),
            icon = Color(0xff4879b3),
            separator = Color(0x3b72839a),
            edge = Color(0x6e9baec5),
            hover = Color(0x6384b2e4),
            pressed = Color(0x886aa7e5),
            selected = Color(0xffd4e7fc),
            selectedStrong = Color(0xff247bd3),
            selectedInactive = Color(0xffe1e6ee),
            selectedText = Color(0xff173a65),
            selectedStrongText = Color.White,
            focusRing = Color(0xff377fe7),
            scrollbar = Color(0x6f657a94),
            scrollbarHover = Color(0xb558738f),
            illuminatedEdge = Color(0x99ffffff),
            feedback = Color(0xfffff2d7),
            progress = Color(0xffeaf3ff),
            dragHint = Color(0xffe6f1ff),
            dark = false,
        )
        val Dark = FinderGlassPalette(
            window = Color(0xff171e2a),
            sidebar = Color(0xef283447),
            titlebar = Color(0xf02a3548),
            toolbar = Color(0xf02a3548),
            content = Color(0xff1d2634),
            footer = Color(0xff202a39),
            ink = Color(0xffedf3fa),
            secondaryInk = Color(0xffadbace),
            accent = Color(0xff8bc0ff),
            icon = Color(0xffaacdf9),
            separator = Color(0x58899ab0),
            edge = Color(0x758ea5c0),
            hover = Color(0x5a547baf),
            pressed = Color(0x796c96cc),
            selected = Color(0xff314f77),
            selectedStrong = Color(0xff367ac8),
            selectedInactive = Color(0xff394252),
            selectedText = Color(0xffeff5ff),
            selectedStrongText = Color.White,
            focusRing = Color(0xffa5cfff),
            scrollbar = Color(0x889cabbf),
            scrollbarHover = Color(0xd9b4c9e0),
            illuminatedEdge = Color(0x417e95b1),
            feedback = Color(0xff4b3e28),
            progress = Color(0xff263c58),
            dragHint = Color(0xff24405c),
            dark = true,
        )
    }

    fun selection(active: Boolean, focusedWindow: Boolean): Color =
        if (!focusedWindow) selectedInactive else if (active) selectedStrong else selected

    fun selectionText(active: Boolean, focusedWindow: Boolean): Color =
        if (focusedWindow && active) selectedStrongText else if (!focusedWindow) ink else selectedText
}

/** Focus is independent from selection; persisted file selection is never rewritten on blur. */
internal class FinderControlFocus {
    private var activeControl by mutableStateOf<String?>(null)
    val hasFocusedControl: Boolean get() = activeControl != null
    fun update(id: String, focused: Boolean) {
        if (focused) activeControl = id
        else if (activeControl == id) activeControl = null
    }
}

internal data class FinderMaterials(
    val palette: FinderGlassPalette,
    val windowFocused: Boolean,
    val reducedMotion: Boolean,
    val desktopBlurSupported: Boolean,
) {
    val hoverMillis: Int get() = if (reducedMotion) 0 else 100
    val selectionMillis: Int get() = if (reducedMotion) 0 else 135
    val surfaceMillis: Int get() = if (reducedMotion) 0 else 185
}

internal val LocalFinderMaterials = compositionLocalOf {
    FinderMaterials(FinderGlassPalette.Light, true, false, false)
}
internal val LocalFinderControlFocus = compositionLocalOf { FinderControlFocus() }

@Composable
internal fun rememberFinderMaterials(): FinderMaterials {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.48f
    var focused by remember(view) { mutableStateOf(view.hasWindowFocus()) }
    DisposableEffect(view) {
        val observer = view.viewTreeObserver
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused = it }
        observer.addOnWindowFocusChangeListener(listener)
        focused = view.hasWindowFocus()
        onDispose { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    }
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    // The compositor may support blur, but Window.setBackgroundBlurRadius additionally
    // REQUIRES a translucent and floating window. DeX's normal resizable Activity does
    // not meet that contract: retain the caption bar and render an opaque-backed tint.
    val eligibleForWindowBlur = remember(context) {
        val floating = TypedValue()
        val translucent = TypedValue()
        val f = context.theme.resolveAttribute(android.R.attr.windowIsFloating, floating, true) &&
            floating.data != 0
        val t = context.theme.resolveAttribute(android.R.attr.windowIsTranslucent, translucent, true) &&
            translucent.data != 0
        f && t
    }
    var compositorEnabled by remember { mutableStateOf(false) }
    DisposableEffect(context, eligibleForWindowBlur) {
        if (Build.VERSION.SDK_INT >= 31 && eligibleForWindowBlur) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (wm != null) {
                val listener = java.util.function.Consumer<Boolean> { compositorEnabled = it }
                wm.addCrossWindowBlurEnabledListener(listener)
                onDispose { wm.removeCrossWindowBlurEnabledListener(listener) }
            } else onDispose {}
        } else onDispose {}
    }
    // Capability is reported only, not activated by changing Android window flags/theme.
    return FinderMaterials(if (isDark) FinderGlassPalette.Dark else FinderGlassPalette.Light,
        focused, reducedMotion, eligibleForWindowBlur && compositorEnabled)
}

@Composable
internal fun finderAnimatedMaterial(target: Color, selected: Boolean = false): Color {
    val materials = LocalFinderMaterials.current
    val color by animateColorAsState(target,
        animationSpec = tween(if (selected) materials.selectionMillis else materials.hoverMillis),
        label = "Finder material transition")
    return color
}

@Composable
internal fun Modifier.finderFocusOutline(focused: Boolean): Modifier {
    val palette = LocalFinderMaterials.current.palette
    return this.then(if (focused) Modifier.border(
        BorderStroke(1.5.dp, palette.focusRing), RoundedCornerShape(8.dp)) else Modifier)
}
