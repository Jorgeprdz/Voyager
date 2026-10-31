package com.voyagerfiles.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Pure hit testing: every selected path comes from a current laid-out FileItem. */
internal fun finderPathsInRect(bounds: Map<String, Rect>, region: Rect): Set<String> =
    bounds.filterValues { it.overlaps(region) }.keys

internal class FinderMarqueeState {
    private val itemBounds = mutableStateMapOf<String, Rect>()
    var active by mutableStateOf(false)
        private set
    var start by mutableStateOf(Offset.Zero)
        private set
    var end by mutableStateOf(Offset.Zero)
        private set
    private var initialSelection = emptySet<String>()
    private var additive = false
    private val exitedDuringScroll = mutableSetOf<String>()
    private var lastVisibleHits = emptySet<String>()

    val rectangle: Rect
        get() = Rect(
            minOf(start.x, end.x), minOf(start.y, end.y),
            maxOf(start.x, end.x), maxOf(start.y, end.y),
        )

    fun register(path: String, bounds: Rect) { itemBounds[path] = bounds }
    fun unregister(path: String) {
        if (active && path in lastVisibleHits) exitedDuringScroll += path
        itemBounds.remove(path)
    }
    fun clearItems() { itemBounds.clear(); cancel() }
    fun hitItem(at: Offset): Boolean = itemBounds.values.any { it.contains(at) }

    fun startDrag(at: Offset, selected: Set<String>, extend: Boolean): Boolean {
        if (hitItem(at)) return false
        start = at
        end = at
        initialSelection = selected
        exitedDuringScroll.clear()
        lastVisibleHits = emptySet()
        additive = extend
        active = true
        return true
    }
    fun update(at: Offset): Set<String> {
        if (!active) return emptySet()
        end = at
        val hits = finderPathsInRect(itemBounds, rectangle)
        lastVisibleHits = hits
        return (if (additive) initialSelection else emptySet()) + hits + exitedDuringScroll
    }
    fun finish() { active = false; exitedDuringScroll.clear(); lastVisibleHits = emptySet() }
    fun cancel() { active = false; exitedDuringScroll.clear(); lastVisibleHits = emptySet() }
}

/** Attach to item content; composable disposal removes stale bounds after scrolling or switching views. */
internal fun Modifier.finderMarqueeItem(path: String, marquee: FinderMarqueeState): Modifier = composed {
    DisposableEffect(marquee, path) { onDispose { marquee.unregister(path) } }
    this.onGloballyPositioned { marquee.register(path, it.boundsInRoot()) }
}

/** Parent gesture only activates if the pointer begins on truly empty canvas. */
internal fun Modifier.finderMarqueeGesture(
    controller: FinderColumnController,
    marquee: FinderMarqueeState,
    drag: FinderDragDropState,
): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var started by remember { mutableStateOf(false) }
    var additive by remember { mutableStateOf(false) }
    this.onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press) additive =
                        event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed ||
                            event.keyboardModifiers.isShiftPressed
                }
            }
        }
        .pointerInput(controller, marquee, drag) {
            detectDragGestures(
                onDragStart = { offset ->
                    val start = origin + offset
                    started = drag.draggingPaths.isEmpty() &&
                        marquee.startDrag(start, controller.columns.value.lastOrNull()?.selected.orEmpty(), additive)
                },
                onDrag = { change, _ ->
                    if (started) {
                        change.consume()
                        controller.selectPaths(marquee.update(origin + change.position))
                    }
                },
                onDragEnd = { if (started) marquee.finish(); started = false },
                onDragCancel = { marquee.cancel(); started = false },
            )
        }
        .pointerInput(controller, marquee) {
            awaitPointerEventScope {
                var pressedAt: Offset? = null
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull()
                    if (event.type == PointerEventType.Press)
                        pressedAt = pointer?.position?.let { origin + it }
                    if (event.type == PointerEventType.Release) {
                        val release = pointer?.position?.let { origin + it }
                        val down = pressedAt
                        if (down != null && release != null &&
                            (release - down).getDistance() < 8f && !marquee.hitItem(down))
                            controller.clearSelection()
                        pressedAt = null
                    }
                }
            }
        }
}

@Composable
internal fun FinderMarqueeOverlay(marquee: FinderMarqueeState, modifier: Modifier = Modifier) {
    if (!marquee.active) return
    val palette = LocalFinderMaterials.current.palette
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Draw-only overlay: never intercept marquee hit-testing, mouse clicks, or drag targets.
    Canvas(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val region = marquee.rectangle
        val local = Offset(region.left - origin.x, region.top - origin.y)
        val rectSize = androidx.compose.ui.geometry.Size(region.width, region.height)
        val radius = CornerRadius(6.dp.toPx())
        drawRoundRect(palette.accent.copy(alpha = if (palette.dark) .24f else .13f),
            topLeft = local, size = rectSize, cornerRadius = radius)
        drawRoundRect(palette.accent.copy(alpha = .72f),
            topLeft = local, size = rectSize, cornerRadius = radius,
            style = Stroke(width = 1.5.dp.toPx()))
    }
}

/** Pure edge velocity in pixels per animation tick; zero outside an active viewport. */
internal fun finderVerticalEdgeSpeed(pointer: Offset, viewport: Rect, zone: Float = 56f): Float {
    if (!pointer.x.isFinite() || !pointer.y.isFinite() || viewport.isEmpty ||
        pointer.x < viewport.left || pointer.x > viewport.right) return 0f
    return when {
        pointer.y >= viewport.top && pointer.y < viewport.top + zone ->
            -22f * (1f - ((pointer.y - viewport.top) / zone)).coerceIn(0f, 1f)
        pointer.y <= viewport.bottom && pointer.y > viewport.bottom - zone ->
            22f * (1f - ((viewport.bottom - pointer.y) / zone)).coerceIn(0f, 1f)
        else -> 0f
    }
}

@Composable
internal fun FinderEdgeAutoScroll(
    marquee: FinderMarqueeState,
    drag: FinderDragDropState,
    viewport: Rect,
    scrollBy: suspend (Float) -> Unit,
) {
    val active = marquee.active || drag.draggingPaths.isNotEmpty()
    LaunchedEffect(active, viewport) {
        if (active && !viewport.isEmpty) while (true) {
            val pointer = if (marquee.active) marquee.end else drag.cursorPosition
            val speed = finderVerticalEdgeSpeed(pointer, viewport)
            if (speed != 0f) scrollBy(speed)
            delay(16L)
        }
    }
}
