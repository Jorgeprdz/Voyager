package com.voyagerfiles.desktop

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import java.io.File

internal enum class FinderDropAction { MOVE, COPY }
internal data class FinderPendingMove(
    val paths: List<String>,
    val destination: String,
    val action: FinderDropAction = FinderDropAction.MOVE,
)
private data class FinderDropTarget(val path: String, val rect: Rect, val allowed: Boolean)

/** Pointer coordinates and visual state are separate from Voyager's real transfer lifecycle. */
internal class FinderDragDropState {
    private val targets = mutableStateMapOf<String, FinderDropTarget>()
    var allowedRoot: String = "/"
    var draggingPaths by mutableStateOf<List<String>>(emptyList())
        private set
    var ghostPaths by mutableStateOf<List<String>>(emptyList())
        private set
    var cursorPosition by mutableStateOf(Offset.Unspecified)
        private set
    private var sourcePosition = Offset.Zero
    var ghostDestination by mutableStateOf(Offset.Zero)
        private set
    var returning by mutableStateOf(false)
        private set
    var receiving by mutableStateOf(false)
        private set
    var copyRequested by mutableStateOf(false)
    var awaitingTransfer by mutableStateOf(false)
        private set
    var hoveredDestination by mutableStateOf<String?>(null)
        private set
    var springFolder by mutableStateOf<String?>(null)
        private set
    var pending by mutableStateOf<FinderPendingMove?>(null)
        private set

    private fun withinRoot(path: String): Boolean =
        allowedRoot == "/" || path == allowedRoot || path.startsWith("$allowedRoot/")

    private fun canTarget(path: String): Boolean =
        withinRoot(path) && File(path).isDirectory && File(path).canWrite()

    fun register(id: String, path: String, rect: Rect) {
        val candidate = FinderDropTarget(path, rect, canTarget(path))
        if (targets[id] != candidate) targets[id] = candidate
    }
    fun unregister(id: String) {
        targets.remove(id)
        if (draggingPaths.isNotEmpty()) resolveHover()
    }
    fun clearTargets() { targets.clear(); hoveredDestination = null }
    fun start(paths: List<String>, at: Offset, from: Offset = at) {
        val valid = paths.distinct().filter(::withinRoot)
        if (valid.isEmpty()) return
        draggingPaths = valid
        ghostPaths = valid
        cursorPosition = at
        sourcePosition = from
        ghostDestination = at
        returning = false
        receiving = false
        pending = null
        resolveHover()
    }
    fun move(at: Offset) {
        if (draggingPaths.isEmpty()) return
        cursorPosition = at
        resolveHover()
    }

    private fun target(): FinderDropTarget? {
        if (draggingPaths.isEmpty() || (!cursorPosition.x.isFinite() || !cursorPosition.y.isFinite())) return null
        return targets.values.asSequence()
            .filter { it.rect.contains(cursorPosition) }
            .sortedBy { it.rect.width * it.rect.height }
            .firstOrNull { candidate ->
                candidate.allowed && draggingPaths.all { source ->
                    withinRoot(source) && source != candidate.path &&
                        !candidate.path.startsWith("$source/") &&
                        File(source).parent != candidate.path
                }
            }
    }
    private fun resolveHover() {
        hoveredDestination = target()?.path
        if (springFolder != null && hoveredDestination == null &&
            draggingPaths.isEmpty()) springFolder = null
    }

    fun openSpringFolder(folder: String) {
        if (hoveredDestination == folder && draggingPaths.isNotEmpty() && canTarget(folder)) {
            springFolder = folder
        }
    }
    fun closeSpringFolder() { springFolder = null }

    fun end() {
        val destination = target()
        if (destination != null) {
            pending = FinderPendingMove(draggingPaths.toList(), destination.path,
                if (copyRequested) FinderDropAction.COPY else FinderDropAction.MOVE)
            ghostDestination = destination.rect.center
            receiving = false
            returning = false
            draggingPaths = emptyList()
            hoveredDestination = null
            springFolder = null
        } else cancel()
    }
    fun acceptPending() {
        if (pending != null) {
            awaitingTransfer = true
            receiving = true
            pending = null
        }
    }
    fun completeTransfer(success: Boolean) {
        awaitingTransfer = false
        pending = null
        if (success) clearGhost()
        else {
            receiving = false
            returning = true
            ghostDestination = sourcePosition
        }
    }
    fun cancel() {
        draggingPaths = emptyList()
        hoveredDestination = null
        springFolder = null
        if (ghostPaths.isNotEmpty()) {
            returning = true
            receiving = false
            ghostDestination = sourcePosition
        }
    }
    fun clearGhost() {
        ghostPaths = emptyList()
        receiving = false
        returning = false
    }
    fun dismiss() {
        pending = null
        if (!awaitingTransfer && ghostPaths.isNotEmpty()) cancel()
    }
}

internal fun Modifier.finderDragSource(
    path: String,
    selection: Set<String>,
    drag: FinderDragDropState,
    copyModifier: Boolean = false,
): Modifier = composed {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    this.onGloballyPositioned { rootOrigin = it.positionInRoot() }
        .pointerInput(path, selection, drag) {
            detectDragGestures(
                onDragStart = { at ->
                    drag.copyRequested = copyModifier
                    drag.start(if (path in selection) selection.toList() else listOf(path),
                        rootOrigin + at, rootOrigin + at)
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
