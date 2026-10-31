package com.voyagerfiles.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.SortBy
import com.voyagerfiles.ui.components.FileThumbnailOrIcon
import java.text.DateFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FinderListView(
    controller: FinderColumnController,
    columns: List<FinderColumn>,
    query: String,
    descending: Boolean,
    sortBy: SortBy,
    modifier: Modifier,
    onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean,
    clipboardAvailable: Boolean,
    onAction: (FinderFileAction, FileItem) -> Unit,
    dragState: FinderDragDropState,
    zoom: FinderViewDensity,
) {
    val last = columns.lastOrNull()
    val files = remember(last?.files, query, descending, sortBy) {
        finderOrderFiles(last?.files.orEmpty().filter { it.name.contains(query, ignoreCase = true) },
            sortBy, descending)
    }
    val marquee = remember(last?.path) { FinderMarqueeState() }
    val glass = LocalFinderMaterials.current
    val palette = glass.palette
    val listState = rememberLazyListState()
    var listBounds by remember { mutableStateOf(Rect.Zero) }
    FinderEdgeAutoScroll(marquee, dragState, listBounds) { listState.scrollBy(it) }
    BoxWithConstraints(modifier.background(palette.content).testTag("finder-list")
        .finderMarqueeGesture(controller, marquee, dragState)) {
    val listColumns = FinderContentTokens.listColumns(maxWidth.value)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(FinderContentTokens.LIST_HEADER_HEIGHT_DP.dp)
            .background(palette.toolbar)
            .padding(start = 21.dp, end = 21.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_column_name),
                Modifier.weight(1f), color = palette.secondaryInk, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (listColumns.showSize) Text(
                androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_column_size),
                Modifier.width(100.dp), color = palette.secondaryInk, fontSize = 11.sp)
            if (listColumns.showType) Text(
                androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_column_type),
                Modifier.width(82.dp), color = palette.secondaryInk, fontSize = 11.sp)
            if (listColumns.showDate) Text(
                androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_column_modified),
                Modifier.width(128.dp), color = palette.secondaryInk, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.separator))
        Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState,
            modifier = Modifier.fillMaxSize().onGloballyPositioned { listBounds = it.boundsInRoot() },
            contentPadding = PaddingValues(vertical = 5.dp)) {
            items(files, key = { it.path }) { file ->
                var menu by remember { mutableStateOf(false) }
                var ctrl by remember { mutableStateOf(false) }
                var shift by remember { mutableStateOf(false) }
                val hoverSource = remember { MutableInteractionSource() }
                val hovered by hoverSource.collectIsHoveredAsState()
                val pressed by hoverSource.collectIsPressedAsState()
                val selected = file.path in last?.selected.orEmpty()
                var keyboardFocused by remember { mutableStateOf(false) }
                val surface = finderAnimatedMaterial(if (pressed) palette.pressed
                    else if (selected) palette.selection(false, glass.windowFocused)
                    else if (hovered) palette.hover else Color.Transparent, selected)
                Row(Modifier.fillMaxWidth().height(zoom.rowHeightDp.dp)
                    .padding(horizontal = 9.dp, vertical = 1.dp)
                    .clip(RoundedCornerShape(FinderContentTokens.LIST_ROW_CORNER_DP.dp))
                    .background(surface).hoverable(hoverSource)
                    .onFocusChanged { keyboardFocused = it.isFocused }
                    .finderMarqueeItem(file.path, marquee)
                    .finderDragVisual(file.path, file.isDirectory, dragState)
                    .finderDragSource(file.path, last?.selected.orEmpty(), dragState, ctrl)
                    .then(if (file.isDirectory) Modifier.finderFolderDropTarget("list:${file.path}", file.path, dragState) else Modifier)
                    .finderMouseSelectionModifiers { c, sh -> ctrl = c; shift = sh }
                    .finderRightClick {
                        if (file.path !in last?.selected.orEmpty()) controller.selectOnly(columns.lastIndex, file)
                        menu = true
                    }
                    .combinedClickable(interactionSource = hoverSource, indication = null,
                        onClick = { controller.selectOnly(columns.lastIndex, file, ctrl, shift, files.map { it.path }) },
                        onDoubleClick = {
                            if (file.isDirectory) controller.select(columns.lastIndex, file) else onOpen(file)
                        },
                        onLongClick = { controller.selectOnly(columns.lastIndex, file); menu = true })
                    .finderFocusOutline(keyboardFocused)
                    .semantics { this.selected = selected }
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(zoom.iconDp.dp))
                    else FileThumbnailOrIcon(file, zoom.iconDp.dp)
                    Spacer(Modifier.width(9.dp))
                    val rowInk = if (selected) palette.selectionText(false, glass.windowFocused) else palette.ink
                    Text(file.name, Modifier.weight(1f), color = rowInk, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    if (listColumns.showSize) Text(if (file.isDirectory) "—" else file.formattedSize,
                        Modifier.width(100.dp), color = rowInk, fontSize = 11.sp, maxLines = 1)
                    if (listColumns.showType) Text(
                        if (file.isDirectory) "DIR" else file.extension.uppercase().ifBlank { "—" },
                        Modifier.width(82.dp), color = rowInk, fontSize = 11.sp, maxLines = 1)
                    if (listColumns.showDate) Text(
                        DateFormat.getDateInstance(DateFormat.SHORT).format(file.lastModified),
                        Modifier.width(128.dp), color = rowInk, fontSize = 11.sp, maxLines = 1)
                    FinderFileMenu(menu, { menu = false }, file.isDirectory, operationsAvailable,
                        if (file.path in last?.selected.orEmpty()) last?.selected?.size ?: 1 else 1, isArchive = file.isArchive, canPaste = clipboardAvailable) {
                        onAction(it, file)
                    }
                }
            }
        }
        FinderLazyListScrollbar(listState, Modifier.align(Alignment.CenterEnd))
        }
    }
    FinderMarqueeOverlay(marquee)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FinderGalleryView(
    controller: FinderColumnController,
    columns: List<FinderColumn>,
    query: String,
    descending: Boolean,
    sortBy: SortBy,
    modifier: Modifier,
    onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean,
    clipboardAvailable: Boolean,
    onAction: (FinderFileAction, FileItem) -> Unit,
    dragState: FinderDragDropState,
    zoom: FinderViewDensity,
) {
    val last = columns.lastOrNull()
    val files = remember(last?.files, query, descending, sortBy) {
        finderOrderFiles(last?.files.orEmpty().filter { it.name.contains(query, ignoreCase = true) },
            sortBy, descending)
    }
    val selected = files.firstOrNull { it.path in last?.selected.orEmpty() }
    val glass = LocalFinderMaterials.current
    val palette = glass.palette
    Column(modifier.background(palette.content).testTag("finder-gallery")) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val heroSize = FinderContentTokens.galleryHeroSize(maxWidth.value, maxHeight.value).dp
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                if (selected == null) {
                    Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_select_file),
                        fontSize = 13.sp, color = palette.secondaryInk)
                } else {
                    Box(Modifier.size(heroSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.footer)
                        .border(0.7.dp, palette.separator, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        if (selected.isDirectory) FinderGlyph("folder",
                            modifier = Modifier.size(heroSize * .77f))
                        else FileThumbnailOrIcon(selected, heroSize * .88f)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(selected.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        color = palette.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(selected.formattedSize, fontSize = 12.sp, color = palette.secondaryInk)
                    if (!selected.isDirectory) Text(
                        androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_double_click_open),
                        fontSize = 11.sp, color = palette.secondaryInk)
                }
            }
        }
        LazyRow(Modifier.fillMaxWidth().height(zoom.stripHeightDp.dp)
            .background(palette.footer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(files, key = { it.path }) { file ->
                var menu by remember { mutableStateOf(false) }
                var ctrl by remember { mutableStateOf(false) }
                var shift by remember { mutableStateOf(false) }
                val active = file.path in last?.selected.orEmpty()
                var keyboardFocused by remember { mutableStateOf(false) }
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val pressed by interaction.collectIsPressedAsState()
                val surface = finderAnimatedMaterial(if (pressed) palette.pressed
                    else if (active) palette.selection(false, glass.windowFocused)
                    else if (hovered) palette.hover else Color.Transparent, active)
                Column(Modifier.width((zoom.iconDp + 36f).dp)
                    .fillMaxHeight().clip(RoundedCornerShape(10.dp))
                    .background(surface).hoverable(interaction)
                    .onFocusChanged { keyboardFocused = it.isFocused }
                    .finderDragVisual(file.path, file.isDirectory, dragState)
                    .finderDragSource(file.path, last?.selected.orEmpty(), dragState, ctrl)
                    .finderMouseSelectionModifiers { c, sh -> ctrl = c; shift = sh }
                    .then(if (file.isDirectory) Modifier.finderFolderDropTarget("gallery:${file.path}", file.path, dragState) else Modifier)
                    .finderRightClick {
                        if (file.path !in last?.selected.orEmpty()) controller.selectOnly(columns.lastIndex, file)
                        menu = true
                    }
                    .finderFocusOutline(keyboardFocused)
                    .semantics { this.selected = active }
                    .combinedClickable(interactionSource = interaction, indication = null,
                        onClick = { controller.selectOnly(columns.lastIndex, file, ctrl, shift, files.map { it.path }) },
                        onDoubleClick = {
                            if (file.isDirectory) controller.select(columns.lastIndex, file) else onOpen(file)
                        },
                        onLongClick = { menu = true })
                    .padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (file.isDirectory) FinderGlyph("folder",
                        modifier = Modifier.size(zoom.iconDp.dp))
                    else FileThumbnailOrIcon(file, zoom.iconDp.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(file.name, color = if (active) palette.selectionText(false, glass.windowFocused) else palette.ink,
                        fontSize = 11.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    FinderFileMenu(menu, { menu = false }, file.isDirectory, operationsAvailable,
                        if (file.path in last?.selected.orEmpty()) last?.selected?.size ?: 1 else 1, isArchive = file.isArchive, canPaste = clipboardAvailable) {
                        onAction(it, file)
                    }
                }
            }
        }
    }
}
