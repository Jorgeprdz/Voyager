package com.voyagerfiles.desktop

import android.os.Environment
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.voyagerfiles.R
import com.voyagerfiles.data.model.SortBy
import kotlinx.coroutines.delay

internal object FinderToolbarTokens {
    const val CAPSULE_HEIGHT_DP = 42f
    const val BUTTON_DP = 40f
    const val CAPSULE_RADIUS_DP = 22f
    const val GROUP_GAP_DP = 9f
    const val TIGHT_GAP_DP = 5f
    const val SEARCH_FIELD_DP = 156f
    const val SEARCH_POPUP_DP = 220f
    const val TOOLTIP_DELAY_MS = 550L
}

internal enum class FinderToolbarMode { WIDE, MEDIUM, COMPACT, TIGHT }

internal data class FinderToolbarPlan(
    val mode: FinderToolbarMode,
    val showTitle: Boolean,
    val showForward: Boolean,
    val fullViewSelector: Boolean,
    val directFileActions: Boolean,
    val directAdvancedActions: Boolean,
    val inlineSearch: Boolean,
    val overflow: Boolean,
) {
    companion object {
        fun resolve(widthDp: Float, searching: Boolean): FinderToolbarPlan {
            val width = widthDp.coerceAtLeast(0f)
            val mode = when {
                width >= 915f -> FinderToolbarMode.WIDE
                width >= 645f -> FinderToolbarMode.MEDIUM
                width >= 305f -> FinderToolbarMode.COMPACT
                else -> FinderToolbarMode.TIGHT
            }
            return FinderToolbarPlan(mode,
                showTitle = mode != FinderToolbarMode.TIGHT,
                showForward = mode != FinderToolbarMode.TIGHT,
                fullViewSelector = mode in listOf(FinderToolbarMode.WIDE, FinderToolbarMode.MEDIUM),
                directFileActions = mode in listOf(FinderToolbarMode.WIDE, FinderToolbarMode.MEDIUM),
                directAdvancedActions = mode == FinderToolbarMode.WIDE,
                inlineSearch = searching && width >= 1120f,
                overflow = mode != FinderToolbarMode.WIDE)
        }
    }
}

@Composable
internal fun FinderToolbarCapsule(content: @Composable RowScope.() -> Unit) {
    val p = LocalFinderMaterials.current.palette
    val shape = RoundedCornerShape(FinderToolbarTokens.CAPSULE_RADIUS_DP.dp)
    Row(Modifier.height(FinderToolbarTokens.CAPSULE_HEIGHT_DP.dp)
        .shadow(2.dp, shape).clip(shape).background(p.titlebar)
        .border(0.7.dp, p.edge, shape).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
internal fun FinderToolbarSeparator() {
    Box(Modifier.width(1.dp).height(19.dp)
        .background(LocalFinderMaterials.current.palette.separator))
}

/** The built-in clickable retains keyboard Enter/Space, Tab order and focusability. */
@Composable
internal fun FinderToolbarButton(
    label: String, glyph: String, enabled: Boolean = true, selected: Boolean = false,
    standalone: Boolean = false, action: () -> Unit,
) {
    val materials = LocalFinderMaterials.current
    val p = materials.palette
    val tracker = LocalFinderControlFocus.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(hovered, focused, enabled) {
        showTooltip = false
        if (enabled && (hovered || focused)) {
            delay(FinderToolbarTokens.TOOLTIP_DELAY_MS)
            showTooltip = true
        }
    }
    val target = when {
        !enabled -> Color.Transparent
        pressed -> p.pressed
        selected -> p.selected
        hovered -> p.hover
        else -> Color.Transparent
    }
    val material by animateColorAsState(target, animationSpec = tween(
        if (materials.reducedMotion) 0 else if (selected) 145 else if (pressed) 90 else 100),
        label = "Finder toolbar state")
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(FinderToolbarTokens.BUTTON_DP.dp)
            .then(if (standalone) Modifier.shadow(2.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .then(if (standalone) Modifier.background(p.titlebar)
                .border(0.7.dp, p.edge, CircleShape) else Modifier)
            .background(material).hoverable(interaction)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .onFocusChanged {
                focused = it.isFocused
                tracker.update("toolbar:$label", it.isFocused)
            }
            .clickable(enabled = enabled, interactionSource = interaction, indication = null,
                onClickLabel = label, onClick = { showTooltip = false; action() })
            .then(if (focused) Modifier.border(1.5.dp, p.focusRing, CircleShape) else Modifier)
            .semantics { contentDescription = label; role = Role.Button; this.selected = selected },
            contentAlignment = Alignment.Center) {
            FinderGlyph(glyph, if (enabled) p.ink else p.secondaryInk.copy(alpha = 0.52f),
                Modifier.size(18.dp))
        }
        if (showTooltip && enabled) Popup(alignment = Alignment.BottomCenter,
            offset = IntOffset(0, 9), properties = PopupProperties(focusable = false)) {
            Box(Modifier.shadow(3.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)).background(p.window)
                .border(0.8.dp, p.separator, RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)) {
                Text(label, color = p.ink, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun FinderToolbarViewSelector(
    style: FinderViewStyle, onStyle: (FinderViewStyle) -> Unit, expanded: Boolean,
) {
    val modes = listOf(
        Triple(FinderViewStyle.ICONS, "icons", stringResource(R.string.finderdex_view_icons)),
        Triple(FinderViewStyle.LIST, "list", "Vista de lista"),
        Triple(FinderViewStyle.COLUMNS, "columns", stringResource(R.string.finderdex_view_columns)),
        Triple(FinderViewStyle.GALLERY, "gallery", "Vista de galería"))
    if (expanded) FinderToolbarCapsule {
        modes.forEach { (kind, icon, title) ->
            FinderToolbarButton(title, icon, selected = style == kind) { onStyle(kind) }
        }
    } else {
        var menu by remember { mutableStateOf(false) }
        Box {
            FinderToolbarButton("Elegir vista", modes.first { it.first == style }.second,
                standalone = true) { menu = true }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                modes.forEach { (kind, _, title) ->
                    DropdownMenuItem(text = { Text(title) },
                        leadingIcon = { if (kind == style) Text("✓") },
                        onClick = { menu = false; onStyle(kind) })
                }
            }
        }
    }
}

@Composable
internal fun FinderToolbarOverflow(
    plan: FinderToolbarPlan,
    currentPath: String, canForward: Boolean, onForward: () -> Unit,
    canCreateFolder: Boolean, onNewFolder: () -> Unit, onTrash: () -> Unit,
    sortBy: SortBy, onSort: (SortBy) -> Unit,
    descending: Boolean, onToggleDirection: () -> Unit,
    preview: Boolean, previewAvailable: Boolean, onTogglePreview: () -> Unit,
) {
    if (!plan.overflow) return
    var menu by remember { mutableStateOf(false) }
    Box {
        FinderToolbarButton("Más acciones", "more", standalone = true) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (!plan.showTitle) DropdownMenuItem(
                text = { Text(currentPath, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                enabled = false, onClick = {})
            if (!plan.showForward) DropdownMenuItem(
                text = { Text("Adelante") }, enabled = canForward,
                onClick = { menu = false; onForward() })
            if (!plan.directFileActions) {
                DropdownMenuItem(text = { Text("Nueva carpeta") }, enabled = canCreateFolder,
                    onClick = { menu = false; onNewFolder() })
                DropdownMenuItem(text = { Text("Papelera") }, enabled = canCreateFolder,
                    onClick = { menu = false; onTrash() })
            }
            if (!plan.directAdvancedActions) {
                DropdownMenuItem(text = { Text("Ordenar por", fontWeight = FontWeight.SemiBold) },
                    enabled = false, onClick = {})
                listOf(SortBy.NAME to "Nombre", SortBy.TYPE to "Tipo",
                    SortBy.DATE to "Fecha", SortBy.SIZE to "Tamaño").forEach { (option, title) ->
                    DropdownMenuItem(text = { Text(title) },
                        leadingIcon = { if (sortBy == option) Text("✓") },
                        onClick = { menu = false; onSort(option) })
                }
                DropdownMenuItem(text = { Text(if (descending) "Orden ascendente" else "Orden descendente") },
                    onClick = { menu = false; onToggleDirection() })
                DropdownMenuItem(
                    text = { Text(if (!previewAvailable) "Vista previa (ampliar ventana)"
                        else if (preview) "Ocultar vista previa" else "Mostrar vista previa") },
                    enabled = previewAvailable,
                    onClick = { menu = false; onTogglePreview() })
            }
        }
    }
}

@Composable
internal fun FinderToolbarSearch(
    searching: Boolean, value: String, onToggle: () -> Unit, onValue: (String) -> Unit,
    inline: Boolean,
) {
    val p = LocalFinderMaterials.current.palette
    val label = stringResource(R.string.finderdex_search_folder)
    val requester = remember { FocusRequester() }
    val fieldFocus = LocalFinderControlFocus.current
    Box(contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FinderToolbarButton(label, "search", selected = searching, standalone = true) { onToggle() }
            if (searching && inline) {
                BasicTextField(value, onValue,
                    Modifier.width(FinderToolbarTokens.SEARCH_FIELD_DP.dp).height(36.dp)
                        .clip(RoundedCornerShape(18.dp)).background(p.content)
                        .border(0.8.dp, p.separator, RoundedCornerShape(18.dp))
                        .focusRequester(requester)
                        .onFocusChanged { fieldFocus.update("toolbar:search-field", it.isFocused) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { contentDescription = label },
                    singleLine = true, textStyle = TextStyle(color = p.ink, fontSize = 13.sp))
                LaunchedEffect(Unit) { requester.requestFocus() }
            }
        }
        if (searching && !inline) DropdownMenu(expanded = true, onDismissRequest = onToggle) {
            BasicTextField(value, onValue,
                Modifier.width(FinderToolbarTokens.SEARCH_POPUP_DP.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp)).background(p.content)
                    .border(0.8.dp, p.separator, RoundedCornerShape(18.dp))
                    .focusRequester(requester)
                        .onFocusChanged { fieldFocus.update("toolbar:search-field", it.isFocused) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics { contentDescription = label },
                singleLine = true, textStyle = TextStyle(color = p.ink, fontSize = 13.sp))
            LaunchedEffect(Unit) { requester.requestFocus() }
        }
    }
}

@Composable
internal fun FinderToolbar(
    controller: FinderColumnController, chrome: FinderWindowChrome,
    sidebarWidth: Float, dividerWidth: Float, windowWidth: Float,
    compact: Boolean, minimal: Boolean,
    viewStyle: FinderViewStyle, setViewStyle: (FinderViewStyle) -> Unit,
    searching: Boolean, search: String, toggleSearch: () -> Unit, setSearch: (String) -> Unit,
    preview: Boolean, togglePreview: () -> Unit,
    descending: Boolean, toggleSort: () -> Unit,
    sortBy: SortBy, setSortBy: (SortBy) -> Unit, onMobile: () -> Unit,
    canCreateFolder: Boolean, onNewFolder: () -> Unit, onTrash: () -> Unit,
    previewAvailable: Boolean,
) {
    val p = LocalFinderMaterials.current.palette
    val leading = chrome.layout.leading / chrome.density
    val trailing = chrome.layout.trailing / chrome.density
    val leftGap = maxOf(FinderWindowGeometry.TOOLBAR_GAP_DP,
        leading - sidebarWidth - dividerWidth + FinderWindowGeometry.CAPTION_CLEARANCE_DP)
    val freeWidth = (windowWidth - sidebarWidth - dividerWidth - leftGap - trailing - 14f)
        .coerceAtLeast(0f)
    val plan = FinderToolbarPlan.resolve(freeWidth, searching)
    val rootTitle = stringResource(R.string.home_storage)
    val currentTitle = if (controller.currentPath == Environment.getExternalStorageDirectory().path)
        rootTitle else controller.currentPath.substringAfterLast('/').ifEmpty { rootTitle }
    val height = maxOf(FinderWindowGeometry.TOOLBAR_HEIGHT_DP, chrome.captionHeight / chrome.density)
    Row(Modifier.fillMaxWidth().height(height.dp).background(p.toolbar)
        .onGloballyPositioned { chrome.reserveInput(it, sidebarWidth + dividerWidth + leftGap,
            if (searching) 380f else 220f) }
        .padding(end = trailing.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width((sidebarWidth + dividerWidth).dp).fillMaxHeight()
            .background(p.titlebar)
            .padding(start = minOf(leading + FinderWindowGeometry.CAPTION_CLEARANCE_DP, sidebarWidth).dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (sidebarWidth > 160f && leading + 42f < sidebarWidth)
                Text(stringResource(R.string.app_name), color = p.secondaryInk, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Spacer(Modifier.width(leftGap.dp))
        val tight = plan.mode == FinderToolbarMode.TIGHT
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Row(Modifier.fillMaxSize().then(if (tight) Modifier.horizontalScroll(rememberScrollState())
                else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    (if (tight) FinderToolbarTokens.TIGHT_GAP_DP else FinderToolbarTokens.GROUP_GAP_DP).dp)) {
                FinderToolbarCapsule {
                    FinderToolbarButton("Atrás", "back", enabled = controller.canBack) { controller.back() }
                    if (plan.showForward) {
                        FinderToolbarSeparator()
                        FinderToolbarButton("Adelante", "forward", enabled = controller.canForward) { controller.forward() }
                    }
                }
                if (plan.showTitle) Text(currentTitle, Modifier.weight(1f),
                    color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis, maxLines = 1)
                FinderToolbarViewSelector(viewStyle, setViewStyle, plan.fullViewSelector)
                if (plan.directFileActions) FinderToolbarCapsule {
                    FinderToolbarButton("Nueva carpeta", "folder-outline", enabled = canCreateFolder) { onNewFolder() }
                    FinderToolbarSeparator()
                    FinderToolbarButton("Papelera", "trash", enabled = canCreateFolder) { onTrash() }
                }
                if (plan.directAdvancedActions) {
                    var sortMenu by remember { mutableStateOf(false) }
                    FinderToolbarCapsule {
                        Box {
                            FinderToolbarButton("Ordenar: " + sortBy.name.lowercase(), "sort") { sortMenu = true }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(SortBy.NAME to "Nombre", SortBy.TYPE to "Tipo",
                                    SortBy.DATE to "Fecha", SortBy.SIZE to "Tamaño").forEach { (opt, label) ->
                                    DropdownMenuItem(text = { Text(label) },
                                        leadingIcon = { if (sortBy == opt) Text("✓") },
                                        onClick = { sortMenu = false; setSortBy(opt) })
                                }
                            }
                        }
                        FinderToolbarSeparator()
                        FinderToolbarButton(if (descending) "Orden ascendente" else "Orden descendente",
                            "sort", selected = descending) { toggleSort() }
                        FinderToolbarSeparator()
                        FinderToolbarButton("Mostrar vista previa", "preview",
                            enabled = previewAvailable, selected = preview) { togglePreview() }
                    }
                }
                FinderToolbarSearch(searching, search, toggleSearch, setSearch, plan.inlineSearch)
                FinderToolbarOverflow(plan, controller.currentPath, controller.canForward,
                    { controller.forward() }, canCreateFolder, onNewFolder, onTrash,
                    sortBy, setSortBy, descending, toggleSort, preview, previewAvailable, togglePreview)
                Spacer(Modifier.width(7.dp))
            }
        }
    }
}
