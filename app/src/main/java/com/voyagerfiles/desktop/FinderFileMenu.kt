package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.*

enum class FinderFileAction {
    OPEN, OPEN_WITH, QUICK_LOOK, COPY, CUT, PASTE_INTO, DUPLICATE, RENAME, ZIP, EXTRACT, INFO, SHARE, FAVORITE, TRASH, DELETE
}

/** A mouse secondary press is not a touch long-press: support both independently. */
internal fun Modifier.finderRightClick(onRightClick: () -> Unit): Modifier =
    pointerInput(onRightClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    event.changes.forEach { it.consume() }
                    onRightClick()
                }
            }
        }
    }

@Composable
internal fun FinderFileMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isDirectory: Boolean,
    operationsAvailable: Boolean,
    selectionCount: Int = 1,
    isArchive: Boolean = false,
    canPaste: Boolean = false,
    onAction: (FinderFileAction) -> Unit,
) {
    val count = selectionCount.coerceAtLeast(1)
    fun many(single: String, plural: String) = if (count == 1) single else "$plural $count elementos"
    FinderGlassMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (count == 1) funAction(FinderFileAction.OPEN, "Abrir", onDismiss, onAction)
        if (!isDirectory && count == 1) {
            funAction(FinderFileAction.OPEN_WITH, "Abrir con…", onDismiss, onAction)
        }
        if (!isDirectory) {
            funAction(FinderFileAction.SHARE, many("Compartir", "Compartir"), onDismiss, onAction)
        }
        if (!isDirectory && count == 1) funAction(FinderFileAction.QUICK_LOOK, "Vista previa   Espacio", onDismiss, onAction)
        if (count == 1) funAction(FinderFileAction.INFO, "Obtener información", onDismiss, onAction)
        if (operationsAvailable) {
            funAction(FinderFileAction.COPY, many("Copiar   Ctrl+C", "Copiar"), onDismiss, onAction)
            funAction(FinderFileAction.CUT, many("Cortar   Ctrl+X", "Cortar"), onDismiss, onAction)
            if (isDirectory && canPaste) funAction(FinderFileAction.PASTE_INTO, "Pegar dentro de esta carpeta", onDismiss, onAction)
            funAction(FinderFileAction.DUPLICATE, many("Duplicar", "Duplicar"), onDismiss, onAction)
            if (count == 1) funAction(FinderFileAction.RENAME, "Renombrar", onDismiss, onAction)
            funAction(FinderFileAction.ZIP, many("Comprimir ZIP", "Comprimir"), onDismiss, onAction)
            if (count == 1 && isArchive) funAction(FinderFileAction.EXTRACT, "Extraer archivo", onDismiss, onAction)
            if (isDirectory && count == 1) funAction(FinderFileAction.FAVORITE, "Añadir a favoritos", onDismiss, onAction)
            funAction(FinderFileAction.TRASH, many("Mover a papelera", "Mover a papelera"), onDismiss, onAction)
            funAction(FinderFileAction.DELETE, many("Eliminar definitivamente…", "Eliminar definitivamente"), onDismiss, onAction)
        }
    }
}

@Composable
private fun funAction(action: FinderFileAction, name: String, onDismiss: () -> Unit,
    callback: (FinderFileAction) -> Unit) {
    val p = LocalFinderMaterials.current.palette
    val destructive = action == FinderFileAction.DELETE
    DropdownMenuItem(
        text = { Text(name, color = if (destructive) androidx.compose.ui.graphics.Color(0xffc94958) else p.ink) },
        leadingIcon = {
            FinderGlyph(when(action) {
                FinderFileAction.COPY, FinderFileAction.CUT -> "document"
                FinderFileAction.PASTE_INTO, FinderFileAction.DUPLICATE -> "folder-outline"
                FinderFileAction.RENAME, FinderFileAction.INFO -> "document"
                FinderFileAction.ZIP, FinderFileAction.EXTRACT -> "file"
                FinderFileAction.TRASH, FinderFileAction.DELETE -> "trash"
                FinderFileAction.SHARE -> "forward"
                FinderFileAction.FAVORITE -> "folder"
                FinderFileAction.QUICK_LOOK -> "preview"
                else -> "file"
            }, modifier = Modifier.size(18.dp))
        },
        onClick = { onDismiss(); callback(action) }
    )
}

/** Reads keyboard modifiers from the actual Android pointer event, not a stale key press. */
internal fun Modifier.finderMouseSelectionModifiers(onChange: (Boolean, Boolean) -> Unit): Modifier =
    pointerInput(onChange) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                    onChange(
                        event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed,
                        event.keyboardModifiers.isShiftPressed,
                    )
                }
            }
        }
    }
