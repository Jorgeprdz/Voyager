package com.voyagerfiles.desktop

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

internal data class FinderPendingMove(val paths: List<String>, val destination: String)
private data class FinderDropTarget(val path: String, val rect: Rect)

/** A drop never deletes anything. It only queues a confirmed move through FileOperationCoordinator. */
internal class FinderDragDropState {
    private val targets = mutableStateMapOf<String, FinderDropTarget>()
    var draggingPaths by mutableStateOf<List<String>>(emptyList())
        private set
    private var position by mutableStateOf(Offset.Unspecified)
    var pending by mutableStateOf<FinderPendingMove?>(null)
        private set

    fun register(id: String, path: String, rect: Rect) {
        val value = FinderDropTarget(path, rect)
        if (targets[id] != value) targets[id] = value
    }
    fun unregister(id: String) { targets.remove(id) }
    fun clearTargets() { targets.clear() }
    fun start(paths: List<String>, at: Offset) {
        draggingPaths = paths.distinct()
        position = at
    }
    fun move(at: Offset) { position = at }

    private fun destination(): String? {
        if (draggingPaths.isEmpty() || !position.isSpecified) return null
        return targets.values
            .filter { it.rect.contains(position) }
            .sortedBy { it.rect.width * it.rect.height }
            .map { it.path }
            .firstOrNull { target ->
                draggingPaths.all { source ->
                    target != source &&
                        !target.startsWith("$source/") &&
                        java.io.File(source).parent != target
                }
            }
    }
    val hoveredDestination: String? get() = destination()

    fun end() {
        destination()?.let { pending = FinderPendingMove(draggingPaths.toList(), it) }
        cancel()
    }
    fun cancel() { draggingPaths = emptyList(); position = Offset.Unspecified }
    fun dismiss() { pending = null }
}

internal fun Modifier.finderDragSource(
    path: String,
    selection: Set<String>,
    drag: FinderDragDropState,
): Modifier = composed {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    this.onGloballyPositioned { rootOrigin = it.positionInRoot() }
        .pointerInput(path, selection, drag) {
            detectDragGestures(
                onDragStart = { at ->
                    drag.start(if (path in selection) selection.toList() else listOf(path), rootOrigin + at)
                },
                onDrag = { change, _ ->
                    change.consume()
                    drag.move(rootOrigin + change.position)
                },
                onDragEnd = { drag.end() },
                onDragCancel = { drag.cancel() },
            )
        }
}

internal fun Modifier.finderFolderDropTarget(
    id: String,
    path: String,
    drag: FinderDragDropState,
): Modifier = composed {
    DisposableEffect(id, drag) { onDispose { drag.unregister(id) } }
    this.onGloballyPositioned { drag.register(id, path, it.boundsInRoot()) }
}
