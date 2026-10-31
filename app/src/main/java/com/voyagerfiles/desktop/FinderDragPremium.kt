package com.voyagerfiles.desktop

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Animated transforms run on layers, not on measured grid size. */
internal fun Modifier.finderDragVisual(
    path: String,
    isFolder: Boolean,
    drag: FinderDragDropState,
): Modifier = composed {
    val dragged = path in drag.draggingPaths
    val hovered = isFolder && drag.hoveredDestination == path
    val materials = LocalFinderMaterials.current
    val p = materials.palette
    val alpha by animateFloatAsState(if (dragged) .40f else 1f,
        animationSpec = if (materials.reducedMotion) snap() else tween(140),
        label = "Finder source opacity")
    val scale by animateFloatAsState(
        if (hovered) 1.08f else if (dragged) 1.05f else 1f,
        animationSpec = if (materials.reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium), label = "Finder destination spring")
    this.graphicsLayer {
        scaleX = scale; scaleY = scale; this.alpha = alpha
        shadowElevation = if (hovered || dragged) 8.dp.toPx() else 0f
        shape = RoundedCornerShape(10.dp)
        clip = false
    }.background(
        if (hovered) p.accent.copy(alpha = if (p.dark) .23f else .13f) else Color.Transparent,
        RoundedCornerShape(10.dp),
    )
}

/** Draws only lightweight original glyphs; never re-decodes file thumbnails while dragging. */
@Composable
internal fun FinderDragGhost(drag: FinderDragDropState, modifier: Modifier = Modifier) {
    val paths = drag.ghostPaths
    if (paths.isEmpty()) return
    val materials = LocalFinderMaterials.current
    val p = materials.palette
    var origin by remember { mutableStateOf(Offset.Zero) }
    val target = when {
        drag.returning || drag.receiving || drag.pending != null -> drag.ghostDestination
        else -> drag.cursorPosition
    }
    val point by animateOffsetAsState(
        targetValue = if (target.x.isFinite() && target.y.isFinite()) target else Offset.Zero,
        animationSpec = if (materials.reducedMotion) snap() else spring(
            stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "Finder floating stack",
    )
    val scale by animateFloatAsState(
        if (drag.receiving) .70f else 1.05f,
        animationSpec = if (materials.reducedMotion) snap() else tween(160),
        label = "Finder ghost scale")
    val alpha by animateFloatAsState(
        if (drag.receiving) .25f else 1f,
        animationSpec = if (materials.reducedMotion) snap() else tween(210),
        label = "Finder ghost fade")
    LaunchedEffect(drag.returning) {
        if (drag.returning) {
            delay(220)
            if (drag.returning) drag.clearGhost()
        }
    }
    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        Box(Modifier.offset {
                IntOffset((point.x - origin.x + 12.dp.toPx()).roundToInt(),
                    (point.y - origin.y + 16.dp.toPx()).roundToInt())
            }.size(112.dp, 88.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
            repeat(minOf(3, paths.size)) { index ->
                val layer = minOf(3, paths.size) - index - 1
                Box(Modifier.offset((layer * 5).dp, (layer * 5).dp)
                    .width(86.dp).height(62.dp)
                    .shadow((6 + layer * 2).dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.footer)
                    .border(1.dp, p.edge, RoundedCornerShape(10.dp))
                    .padding(8.dp)) {
                    Column {
                        FinderGlyph("file", p.icon, Modifier.size(25.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(paths.first().substringAfterLast('/'), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontSize = 10.sp,
                            color = p.ink)
                    }
                }
            }
            if (paths.size > 1) Box(Modifier.align(Alignment.TopEnd)
                .clip(RoundedCornerShape(30.dp)).background(p.selectedStrong)
                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                Text(paths.size.toString(), color = p.selectedStrongText, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            if (drag.copyRequested) Box(Modifier.align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(6.dp)).background(p.selectedStrong)
                .padding(horizontal = 6.dp, vertical = 1.dp)) {
                Text("+", color = p.selectedStrongText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** A spring-folder hover is provisional and never modifies the original directory. */
@Composable
internal fun FinderSpringFolderTimer(drag: FinderDragDropState, delayMillis: Long = 700L) {
    val hovered = drag.hoveredDestination
    val active = drag.draggingPaths.isNotEmpty()
    LaunchedEffect(hovered, active) {
        if (hovered != null && active) {
            delay(delayMillis)
            if (drag.hoveredDestination == hovered && drag.draggingPaths.isNotEmpty())
                drag.openSpringFolder(hovered)
        }
    }
}
