package com.voyagerfiles.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Scrollbars are *interactive* thumb overlays. Only the narrow thumb receives pointer
 * events; wheel scrolling, selection rectangles, and file drag gestures remain on the
 * original scrolling surface. The track adds no measurement width.
 */
@Composable
private fun FinderScrollThumb(
    visibleFraction: Float,
    scrollFraction: Float,
    scrolling: Boolean,
    onScrollToFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visibleFraction >= 0.999f || visibleFraction <= 0f) return
    val colors = LocalFinderMaterials.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val color by animateColorAsState(
        if (hovered || dragging || scrolling) colors.palette.scrollbarHover
        else colors.palette.scrollbar,
        animationSpec = tween(colors.surfaceMillis),
        label = "Finder scrollbar color",
    )
    val latestScroll by rememberUpdatedState(scrollFraction)
    val latestAction by rememberUpdatedState(onScrollToFraction)
    BoxWithConstraints(modifier.fillMaxHeight().width(12.dp)) {
        val trackHeightPx = constraints.maxHeight.toFloat()
        if (trackHeightPx <= 0f) return@BoxWithConstraints
        val thumbFraction = visibleFraction.coerceIn(0.08f, 1f)
        val thumbHeight = maxHeight * thumbFraction
        val travelPx = trackHeightPx * (1f - thumbFraction)
        val y = with(androidx.compose.ui.platform.LocalDensity.current) {
            (travelPx * scrollFraction.coerceIn(0f, 1f)).toDp()
        }
        Box(
            Modifier.align(Alignment.TopCenter)
                .offset(y = y)
                .width(12.dp)
                .height(thumbHeight)
                .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(travelPx) {
                    var tracking = 0f
                    detectDragGestures(
                        onDragStart = { tracking = latestScroll; dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, delta ->
                        change.consume()
                        if (travelPx > 0f) {
                            tracking = (tracking + delta.y / travelPx).coerceIn(0f, 1f)
                            latestAction(tracking)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(if (hovered || dragging) 7.dp else 5.dp)
                .fillMaxHeight().background(color, RoundedCornerShape(100)))
        }
    }
}

@Composable
internal fun FinderVerticalScrollbar(state: ScrollState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val max = state.maxValue
    val viewport = state.viewportSize
    val fraction = if (max <= 0) 1f else viewport.toFloat() / (viewport + max).coerceAtLeast(1)
    FinderScrollThumb(fraction, if (max <= 0) 0f else state.value.toFloat() / max,
        state.isScrollInProgress, { f -> scope.launch { state.scrollTo((max * f).roundToInt()) } }, modifier)
}

@Composable
internal fun FinderLazyListScrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val info = state.layoutInfo
    val count = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    if (count <= visible || visible == 0) return
    val maxIndex = (count - visible).coerceAtLeast(1)
    val fraction = visible.toFloat() / count
    val progress = state.firstVisibleItemIndex.toFloat() / maxIndex
    val scope = rememberCoroutineScope()
    FinderScrollThumb(fraction, progress, state.isScrollInProgress,
        { f -> scope.launch { state.scrollToItem((f * maxIndex).roundToInt()) } }, modifier)
}

@Composable
internal fun FinderLazyGridScrollbar(state: LazyGridState, modifier: Modifier = Modifier) {
    val info = state.layoutInfo
    val count = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    if (count <= visible || visible == 0) return
    val maxIndex = (count - visible).coerceAtLeast(1)
    val scope = rememberCoroutineScope()
    FinderScrollThumb(visible.toFloat() / count,
        state.firstVisibleItemIndex.toFloat() / maxIndex,
        state.isScrollInProgress,
        { f -> scope.launch { state.scrollToItem((f * maxIndex).roundToInt()) } }, modifier)
}
