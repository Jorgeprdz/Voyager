package com.voyagerfiles.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*

enum class FinderFileAction {
    OPEN, OPEN_WITH, QUICK_LOOK, COPY, CUT, PASTE_INTO, DUPLICATE, RENAME, ZIP, INFO, SHARE, FAVORITE, TRASH, DELETE
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
    onAction: (FinderFileAction) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        funAction(FinderFileAction.OPEN, "Abrir", onDismiss, onAction)
        if (!isDirectory) {
            funAction(FinderFileAction.OPEN_WITH, "Abrir con…", onDismiss, onAction)
            funAction(FinderFileAction.SHARE, "Compartir", onDismiss, onAction)
        }
        if (!isDirectory) funAction(FinderFileAction.QUICK_LOOK, "Vista previa   Espacio", onDismiss, onAction)
        funAction(FinderFileAction.INFO, "Obtener información", onDismiss, onAction)
        if (operationsAvailable) {
            funAction(FinderFileAction.COPY, "Copiar   Ctrl+C", onDismiss, onAction)
            funAction(FinderFileAction.CUT, "Cortar   Ctrl+X", onDismiss, onAction)
            if (isDirectory) funAction(FinderFileAction.PASTE_INTO, "Pegar dentro de esta carpeta", onDismiss, onAction)
            funAction(FinderFileAction.DUPLICATE, "Duplicar", onDismiss, onAction)
            funAction(FinderFileAction.RENAME, "Renombrar", onDismiss, onAction)
            funAction(FinderFileAction.ZIP, "Comprimir ZIP", onDismiss, onAction)
            if (isDirectory) funAction(FinderFileAction.FAVORITE, "Añadir a favoritos", onDismiss, onAction)
            funAction(FinderFileAction.TRASH, "Mover a papelera", onDismiss, onAction)
            funAction(FinderFileAction.DELETE, "Eliminar definitivamente…", onDismiss, onAction)
        }
    }
}

@Composable
private fun funAction(action: FinderFileAction, name: String, onDismiss: () -> Unit,
    callback: (FinderFileAction) -> Unit) {
    DropdownMenuItem(text = { Text(name) }, onClick = { onDismiss(); callback(action) })
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
