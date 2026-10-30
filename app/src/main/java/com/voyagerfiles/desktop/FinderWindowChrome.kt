package com.voyagerfiles.desktop

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView

/** Uses real DeX/window-manager buttons and leaves unclaimed header space draggable. */
internal class FinderWindowChrome(val layout: FinderCaptionLayout, val captionHeight: Int, val density: Float, private val view: View) {
    fun reserveInput(coordinates: LayoutCoordinates, sidebarWidth: Float, actionWidth: Float) {
        if (Build.VERSION.SDK_INT < 35 || !layout.integrated) return
        val origin = coordinates.positionInWindow()
        val y = origin.y.toInt()
        val bottom = minOf(y + coordinates.size.height, captionHeight)
        if (bottom <= y) return
        val navLeft = (origin.x + sidebarWidth * density).toInt()
        val right = (origin.x + coordinates.size.width - layout.trailing).toInt()
        view.systemGestureExclusionRects = listOf(
            Rect(navLeft, y, navLeft + (96 * density).toInt(), bottom),
            Rect(maxOf(navLeft + (96 * density).toInt(), right - (actionWidth * density).toInt()), y, right, bottom),
        ).filter { it.width() > 0 }
    }
}

@Composable
internal fun rememberFinderWindowChrome(): FinderWindowChrome {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var layout by remember(view) { mutableStateOf(FinderCaptionLayout()) }
    var height by remember(view) { mutableIntStateOf(0) }
    DisposableEffect(view, context) {
        val window = context.activity()?.window
        val mask = if (Build.VERSION.SDK_INT >= 35) WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND or
            WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS else 0
        val previous = if (Build.VERSION.SDK_INT >= 35) window?.insetsController?.systemBarsAppearance?.and(mask) ?: 0 else 0
        if (Build.VERSION.SDK_INT >= 35) window?.insetsController?.setSystemBarsAppearance(mask, mask)
        fun update() {
            if (Build.VERSION.SDK_INT >= 35) {
                val insets = view.rootWindowInsets
                val caption = WindowInsets.Type.captionBar()
                val top = insets?.getInsets(caption)?.top ?: 0
                height = top
                layout = finderCaptionLayout(view.rootView.width, top,
                    insets?.getBoundingRects(caption).orEmpty().map { CaptionBounds(it.left, it.right) })
            }
        }
        val listener = ViewTreeObserver.OnGlobalLayoutListener { update() }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        update()
        onDispose {
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            if (Build.VERSION.SDK_INT >= 35) {
                window?.insetsController?.setSystemBarsAppearance(previous, mask)
                view.systemGestureExclusionRects = emptyList()
            }
        }
    }
    return FinderWindowChrome(layout, height, density, view)
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeIf { it !== this }?.activity()
    else -> null
}
