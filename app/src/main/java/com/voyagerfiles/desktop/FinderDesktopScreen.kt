package com.voyagerfiles.desktop

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voyagerfiles.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.*
import com.voyagerfiles.ui.components.FileThumbnailOrIcon
import com.voyagerfiles.util.FileUtils
import com.voyagerfiles.viewmodel.FileBrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat

private val Ink = Color(0xff303034)
private val Muted = Color(0xff77777e)
private val Line = Color(0xffd8d8dc)
private val Accent = Color(0xff087cf1)
private val Sidebar = Color(0xffe9e9ed)
private val Paper = Color(0xfffdfdfd)

@Composable
private fun Label(text: String, modifier: Modifier = Modifier, color: Color = Ink, size: Int = 13, bold: Boolean = false) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size.sp,
        fontFamily = FontFamily.SansSerif, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Original vector shapes; no Apple artwork is bundled. */
@Composable
private fun FinderGlyph(kind: String, color: Color = Muted, modifier: Modifier = Modifier.size(17.dp)) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * .075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            "folder" -> {
                val p = Path().apply {
                    moveTo(w*.04f,h*.22f); lineTo(w*.4f,h*.22f); lineTo(w*.5f,h*.34f)
                    lineTo(w*.96f,h*.34f); lineTo(w*.96f,h*.85f); lineTo(w*.04f,h*.85f); close()
                }
                drawPath(p, Brush.verticalGradient(listOf(Color(0xff80d5f7), Color(0xff37a9e7))))
                drawLine(Color(0xffa5e5ff), Offset(w*.08f,h*.42f), Offset(w*.92f,h*.42f), w*.04f)
            }
            "back", "forward", "down" -> {
                val p = Path().apply {
                    if (kind == "down") { moveTo(w*.25f,h*.4f); lineTo(w*.5f,h*.65f); lineTo(w*.75f,h*.4f) }
                    else { val a = if (kind == "back") .65f else .35f; val b = 1-a
                        moveTo(w*a,h*.2f); lineTo(w*b,h*.5f); lineTo(w*a,h*.8f) }
                }; drawPath(p, color, style = stroke)
            }
            "columns" -> {
                drawRoundRect(color, Offset(w*.05f,h*.15f), androidx.compose.ui.geometry.Size(w*.9f,h*.7f), androidx.compose.ui.geometry.CornerRadius(w*.08f), style = stroke)
                listOf(.35f,.65f).forEach { drawLine(color,Offset(w*it,h*.17f),Offset(w*it,h*.83f),w*.07f) }
            }
            "search" -> {
                drawCircle(color, w*.28f, Offset(w*.4f,h*.4f), style = stroke)
                drawLine(color,Offset(w*.62f,h*.62f),Offset(w*.9f,h*.9f),w*.08f)
            }
            "preview" -> {
                drawRect(color, Offset(w*.08f,h*.2f), androidx.compose.ui.geometry.Size(w*.84f,h*.6f), style = stroke)
                drawLine(color,Offset(w*.62f,h*.2f),Offset(w*.62f,h*.8f),w*.07f)
            }
            "sort" -> listOf(.25f,.5f,.75f).forEachIndexed { i, y -> drawLine(color,Offset(w*.18f,h*y),Offset(w*(.85f-i*.15f),h*y),w*.07f) }
            "home" -> {
                val p = Path().apply { moveTo(w*.12f,h*.46f); lineTo(w*.5f,h*.12f); lineTo(w*.88f,h*.46f)
                    moveTo(w*.25f,h*.4f); lineTo(w*.25f,h*.86f); lineTo(w*.75f,h*.86f); lineTo(w*.75f,h*.4f) }
                drawPath(p,color,style=stroke)
            }
            "drive" -> drawRoundRect(color,Offset(w*.08f,h*.18f),androidx.compose.ui.geometry.Size(w*.84f,h*.65f),androidx.compose.ui.geometry.CornerRadius(w*.1f),style=stroke)
            else -> {
                drawRoundRect(color,Offset(w*.2f,h*.1f),androidx.compose.ui.geometry.Size(w*.6f,h*.8f),androidx.compose.ui.geometry.CornerRadius(w*.06f),style=stroke)
                drawLine(color,Offset(w*.34f,h*.45f),Offset(w*.66f,h*.45f),w*.05f)
                drawLine(color,Offset(w*.34f,h*.62f),Offset(w*.66f,h*.62f),w*.05f)
            }
        }
    }
}

@Composable
private fun Tool(label: String, glyph: String, enabled: Boolean = true, selected: Boolean = false, action: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(Modifier.size(32.dp, 28.dp).clip(RoundedCornerShape(6.dp))
        .background(if (selected) Color(0xffd3d3d8) else if (hovered && enabled) Color(0xffe0e0e4) else Color.Transparent)
        .hoverable(interaction).clickable(enabled = enabled, onClickLabel = label, onClick = action)
        .semantics { contentDescription = label; role = Role.Button }, contentAlignment = Alignment.Center) {
        FinderGlyph(glyph, if (enabled) Ink else Color(0xffbcbcc1))
    }
}

@Composable
fun FinderDesktopHost(viewModel: FileBrowserViewModel, mode: DesktopMode, onMobile: () -> Unit) {
    val session by viewModel.activeSession.collectAsState()
    val browser by viewModel.browseState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(session?.id) {
        if (session == null) viewModel.openLocalRoot(Environment.getExternalStorageDirectory().path)
    }
    val controller = remember(session?.id) { viewModel.finderColumns() }
    LaunchedEffect(controller) { controller?.start() }
    // Desktop navigation is published back to the shared session; external/mobile changes are reconciled.
    LaunchedEffect(browser.currentPath, browser.selectedFiles, browser.isLoading, controller) {
        if (!browser.isLoading && controller != null) controller.reconcile(browser.currentPath, browser.selectedFiles)
    }
    if (controller == null) {
        Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) { Label("FinderDex…") }
        return
    }
    val bookmarks by viewModel.bookmarks.collectAsState()
    var volumes by remember(context) { mutableStateOf(emptyList<com.voyagerfiles.util.StorageDirectory>()) }
    LaunchedEffect(context) {
        volumes = withContext(Dispatchers.IO) { FileUtils.getStorageDirectories(context) }
    }
    var message by remember { mutableStateOf<String?>(null) }
    FinderDesktopScreen(controller, mode, bookmarks.filter { it.source == FileSource.LOCAL }.map { it.name to it.path },
        volumes.map { it.name to it.path }, viewModel::openLocalRoot, onMobile,
        onOpen = { file -> FileUtils.openFile(context, file).onFailure { message = it.localizedMessage ?: "No se pudo abrir el archivo" } },
        message = message, onDismissMessage = { message = null })
}

@Composable
fun FinderDesktopScreen(
    controller: FinderColumnController,
    mode: DesktopMode,
    bookmarks: List<Pair<String, String>> = emptyList(),
    volumes: List<Pair<String, String>> = emptyList(),
    onRoot: (String) -> Unit = {},
    onMobile: () -> Unit = {},
    onOpen: (FileItem) -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
) {
    val columns by controller.columns.collectAsState()
    var sidebarWidth by remember { mutableFloatStateOf(200f) }
    var showPreview by remember { mutableStateOf(mode == DesktopMode.FULL) }
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var descending by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density
    BackHandler(controller.canBack) { controller.back() }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(6.dp)
        .shadow(7.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Paper).border(1.dp, Line, RoundedCornerShape(10.dp))
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false
            else when {
                event.key == Key.Escape && searching -> { searching = false; search = ""; true }
                event.isAltPressed && event.key == Key.DirectionLeft -> { controller.back(); true }
                event.isAltPressed && event.key == Key.DirectionRight -> { controller.forward(); true }
                event.key == Key.Enter -> { controller.selectedFile?.takeUnless { it.isDirectory }?.let(onOpen); true }
                else -> false
            }
        }) {
        FinderToolbar(controller, sidebarWidth, searching, search, { searching = !searching; if (!searching) search = "" }, { search = it },
            showPreview, { showPreview = !showPreview }, descending, { descending = !descending }, onMobile)
        Row(Modifier.weight(1f)) {
            FinderSidebar(controller.currentPath, sidebarWidth.dp, bookmarks, volumes, onRoot)
            Box(Modifier.fillMaxHeight().width(5.dp).background(Sidebar).pointerInput(density) {
                detectHorizontalDragGestures { change, delta -> change.consume(); sidebarWidth = (sidebarWidth + delta/density).coerceIn(150f, 300f) }
            }) { Box(Modifier.fillMaxHeight().width(1.dp).align(Alignment.CenterEnd).background(Line)) }
            FinderWorkspace(controller, columns, search, descending, Modifier.weight(1f), onOpen)
            if (showPreview && mode == DesktopMode.FULL) FinderPreview(controller.selectedFile, onOpen)
        }
        if (message != null) Row(Modifier.fillMaxWidth().background(Color(0xffffefcd)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(message, Modifier.weight(1f)); Label("Cerrar", Modifier.clickable(onClick = onDismissMessage).padding(4.dp), color = Accent)
        }
        FinderStatusBar(controller, columns)
    }
}

@Composable
private fun FinderToolbar(controller: FinderColumnController, sidebarWidth: Float, searching: Boolean, search: String,
    toggleSearch: () -> Unit, setSearch: (String) -> Unit, preview: Boolean, togglePreview: () -> Unit,
    descending: Boolean, toggleSort: () -> Unit, onMobile: () -> Unit) {
    val columnsDescription = stringResource(R.string.finderdex_view_columns)
    val searchDescription = stringResource(R.string.finderdex_search_folder)
    Row(Modifier.fillMaxWidth().height(53.dp).background(Color(0xfff0f0f3)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Label("FinderDex", Modifier.width((sidebarWidth - 7f).dp), size = 14, bold = true)
        Tool("Atrás", "back", controller.canBack) { controller.back() }
        Tool("Adelante", "forward", controller.canForward) { controller.forward() }
        Spacer(Modifier.width(12.dp))
        Label(controller.currentPath.substringAfterLast('/').ifEmpty { "Almacenamiento" }, Modifier.weight(1f), size = 15, bold = true)
        FinderGlyph("columns", Ink, Modifier.size(30.dp, 20.dp).semantics { contentDescription = columnsDescription })
        Spacer(Modifier.width(12.dp))
        Tool(if (descending) "Orden ascendente" else "Orden descendente", "sort", selected = descending, action = toggleSort)
        Tool("Mostrar vista previa", "preview", selected = preview, action = togglePreview)
        Tool(searchDescription, "search", selected = searching, action = toggleSearch)
        if (searching) BasicTextField(search, setSearch, Modifier.padding(start = 5.dp).width(140.dp).background(Color.White, RoundedCornerShape(5.dp)).padding(5.dp)
            .semantics { contentDescription = searchDescription }, singleLine = true, textStyle = TextStyle(color = Ink, fontSize = 13.sp))
        Label("Móvil", Modifier.padding(start = 12.dp).clickable(onClick = onMobile).padding(4.dp), color = Muted, size = 11)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun FinderSidebar(currentPath: String, width: Dp, bookmarks: List<Pair<String, String>>, volumes: List<Pair<String, String>>, onRoot: (String) -> Unit) {
    val root = Environment.getExternalStorageDirectory().path
    Column(Modifier.width(width).fillMaxHeight().background(Sidebar).verticalScroll(rememberScrollState()).padding(10.dp, 12.dp)) {
        var favoritesExpanded by remember { mutableStateOf(true) }
        var locationsExpanded by remember { mutableStateOf(true) }
        SidebarHeading("Favoritos", favoritesExpanded) { favoritesExpanded = !favoritesExpanded }
        if (favoritesExpanded) {
            SidebarItem("Inicio", root, "home", currentPath == root, onRoot)
            listOf("Descargas" to "Download", "Documentos" to "Documents", "Imágenes" to "Pictures", "Música" to "Music", "Películas" to "Movies").forEach { (name, folder) ->
                val path = java.io.File(root, folder).path
                SidebarItem(name, path, "folder", currentPath == path || currentPath.startsWith("$path/"), onRoot)
            }
            bookmarks.forEach { (name, path) -> SidebarItem(name, path, "folder", currentPath == path, onRoot) }
        }
        Spacer(Modifier.height(20.dp))
        SidebarHeading("Ubicaciones", locationsExpanded) { locationsExpanded = !locationsExpanded }
        if (locationsExpanded) volumes.forEach { (name, path) -> SidebarItem(name, path, "drive", currentPath == path, onRoot) }
    }
}

@Composable
private fun SidebarHeading(text: String, expanded: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(7.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(text, Modifier.weight(1f), Muted, 11, true)
        FinderGlyph(if (expanded) "down" else "forward", modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun SidebarItem(name: String, path: String, glyph: String, selected: Boolean, onRoot: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(Modifier.fillMaxWidth().height(29.dp).clip(RoundedCornerShape(5.dp))
        .background(if (selected) Color(0xffcfcfd5) else if (hovered) Color(0xffddddE3) else Color.Transparent)
        .hoverable(interaction).clickable { onRoot(path) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        FinderGlyph(glyph, Accent); Spacer(Modifier.width(8.dp)); Label(name)
    }
}

@Composable
private fun FinderWorkspace(controller: FinderColumnController, columns: List<FinderColumn>, query: String, descending: Boolean, modifier: Modifier, onOpen: (FileItem) -> Unit) {
    val horizontal = rememberScrollState()
    LaunchedEffect(columns.size) { horizontal.animateScrollTo(horizontal.maxValue) }
    Row(modifier.fillMaxHeight().horizontalScroll(horizontal)) {
        columns.forEachIndexed { index, column ->
            key(column.id) {
                ColumnView(controller, index, column, index == columns.indexOfLast { it.selected.isNotEmpty() }, if (index == columns.lastIndex) query else "", descending, onOpen)
            }
        }
        Spacer(Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnView(controller: FinderColumnController, index: Int, column: FinderColumn, active: Boolean, query: String, descending: Boolean, onOpen: (FileItem) -> Unit) {
    val scroll = rememberLazyListState(column.scrollIndex, column.scrollOffset)
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.firstVisibleItemIndex to scroll.firstVisibleItemScrollOffset }.collect { (i, offset) -> controller.rememberScroll(index, i, offset) }
    }
    Row(Modifier.width(228.dp).fillMaxHeight()) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val filtered = remember(column.files, query, descending) {
                column.files.filter { it.name.contains(query, true) }.let { if (descending) it.reversed() else it }
            }
            LazyColumn(state = scroll, modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp), contentPadding = PaddingValues(vertical = 7.dp)) {
                itemsIndexed(filtered, key = { _, f -> f.path }) { _, file ->
                    val selected = file.path in column.selected
                    val interaction = remember { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    Row(Modifier.fillMaxWidth().height(25.dp).clip(RoundedCornerShape(4.dp))
                        .background(if (selected) { if (active) Accent else Color(0xffdedee3) }
                            else if (hovered) Color(0xffefeff2) else Color.Transparent)
                        .hoverable(interaction).combinedClickable(
                            onClick = { controller.select(index, file) },
                            onDoubleClick = { if (file.isDirectory) controller.select(index, file) else onOpen(file) })
                        .semantics { this.selected = selected }.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        FinderGlyph(if (file.isDirectory) "folder" else "file", if (selected && active) Color.White else Muted, Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Label(file.name, Modifier.weight(1f), if (selected && active) Color.White else Ink)
                        if (file.isDirectory) FinderGlyph("forward", if (selected && active) Color.White else Muted, Modifier.size(10.dp))
                    }
                }
            }
            if (column.loading) Label("Cargando…", Modifier.align(Alignment.Center), Muted)
            else if (column.error != null) Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(column.error, style = TextStyle(color = Muted, fontSize = 12.sp))
                Label("Reintentar", Modifier.clickable { controller.refresh() }.padding(10.dp), Accent)
            } else if (filtered.isEmpty()) Label(if (query.isEmpty()) "Carpeta vacía" else "Sin resultados", Modifier.align(Alignment.Center), Muted)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
    }
}

@Composable
private fun FinderPreview(file: FileItem?, onOpen: (FileItem) -> Unit) {
    Row(Modifier.width(230.dp).fillMaxHeight()) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp))
            if (file == null) Label("Vista previa", color = Muted)
            else {
                if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(112.dp))
                else FileThumbnailOrIcon(file, 150.dp)
                Spacer(Modifier.height(24.dp))
                Label(file.name, bold = true)
                Spacer(Modifier.height(6.dp))
                Label(if (file.isDirectory) "Carpeta" else file.formattedSize, color = Muted, size = 12)
                Spacer(Modifier.height(22.dp))
                PreviewDetail("Nombre", file.name)
                PreviewDetail("Clase", if (file.isDirectory) "Carpeta" else file.extension.uppercase().ifEmpty { "Archivo" })
                PreviewDetail("Modificado", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(file.lastModified))
                if (!file.isDirectory) Label("Abrir", Modifier.padding(top = 20.dp).border(1.dp, Line, RoundedCornerShape(5.dp))
                    .clickable { onOpen(file) }.padding(18.dp, 5.dp), color = Ink)
            }
        }
    }
}

@Composable
private fun PreviewDetail(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Label(title, color = Muted, size = 10)
        BasicText(value, style = TextStyle(color = Ink, fontSize = 12.sp))
    }
}

@Composable
private fun FinderStatusBar(controller: FinderColumnController, columns: List<FinderColumn>) {
    val path = controller.currentPath
    var free by remember(path) { mutableStateOf<Long?>(null) }
    LaunchedEffect(path) {
        free = withContext(Dispatchers.IO) { runCatching { android.os.StatFs(path).availableBytes }.getOrNull() }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    Row(Modifier.fillMaxWidth().height(25.dp).background(Color(0xfff5f5f7)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        columns.forEachIndexed { i, c ->
            if (i > 0) Label(" › ", color = Muted, size = 11)
            Label(c.path.substringAfterLast('/').ifEmpty { "/" }, Modifier.clickable { controller.navigateColumn(i) }.widthIn(max = 110.dp), size = 11)
        }
        Spacer(Modifier.weight(1f))
        val current = columns.lastOrNull()
        Label("${current?.files?.size ?: 0} elementos" + if (current?.selected?.isNotEmpty() == true) ", ${current.selected.size} seleccionado" else "", color = Muted, size = 11)
        free?.let { Label(" · ${FileItem.formatFileSize(it)} disponibles", color = Muted, size = 11) }
    }
}
