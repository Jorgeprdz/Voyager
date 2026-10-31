package com.voyagerfiles.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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

    val rectangle: Rect
        get() = Rect(
            minOf(start.x, end.x), minOf(start.y, end.y),
            maxOf(start.x, end.x), maxOf(start.y, end.y),
        )

    fun register(path: String, bounds: Rect) { itemBounds[path] = bounds }
    fun unregister(path: String) { itemBounds.remove(path) }
    fun clearItems() { itemBounds.clear(); cancel() }
    fun hitItem(at: Offset): Boolean = itemBounds.values.any { it.contains(at) }

    fun startDrag(at: Offset, selected: Set<String>, extend: Boolean): Boolean {
        if (hitItem(at)) return false
        start = at
        end = at
        initialSelection = selected
        additive = extend
        active = true
        return true
    }
    fun update(at: Offset): Set<String> {
        if (!active) return emptySet()
        end = at
        val hits = finderPathsInRect(itemBounds, rectangle)
        return if (additive) initialSelection + hits else hits
    }
    fun finish() { active = false }
    fun cancel() { active = false }
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
            detectTapGestures(onTap = { at ->
                if (!marquee.hitItem(origin + at)) controller.clearSelection()
            })
        }
}

@Composable
internal fun FinderMarqueeOverlay(marquee: FinderMarqueeState, modifier: Modifier = Modifier) {
    if (!marquee.active) return
    var origin by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val region = marquee.rectangle
        val local = Offset(region.left - origin.x, region.top - origin.y)
        val rectSize = androidx.compose.ui.geometry.Size(region.width, region.height)
        drawRect(Color(0xff087cf1).copy(alpha = .13f), topLeft = local, size = rectSize)
        drawRect(Color(0xff087cf1).copy(alpha = .7f), topLeft = local,
            size = rectSize, style = Stroke(width = 1.5f))
    }
}
