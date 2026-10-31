package com.voyagerfiles.desktop

import android.os.Environment
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.voyagerfiles.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.*
import com.voyagerfiles.ui.components.FileThumbnailOrIcon
import com.voyagerfiles.util.FileUtils
import com.voyagerfiles.viewmodel.FileBrowserViewModel
import com.voyagerfiles.ui.text.resolve
import com.voyagerfiles.ui.components.TransferConflictDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat

private val Ink = Color(0xff303034)
private val Muted = Color(0xff77777e)
private val Line = Color(0xffe4e4e8)
private val Accent = Color(0xff087cf1)
private val Sidebar = Color(0xeef0f0f3)
private val Header = Color(0xf9f7f7f9)
private val Paper = Color.White

@Composable
private fun Label(text: String, modifier: Modifier = Modifier, color: Color = Ink, size: Int = 13, bold: Boolean = false) {
    val palette = LocalFinderMaterials.current.palette
    val resolved = when (color) { Ink -> palette.ink; Muted -> palette.secondaryInk; Accent -> palette.accent; else -> color }
    BasicText(text, modifier, style = TextStyle(color = resolved, fontSize = size.sp,
        fontFamily = FontFamily.SansSerif, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Original vector shapes; no Apple artwork is bundled. */
@Composable
internal fun FinderGlyph(kind: String, color: Color = Muted, modifier: Modifier = Modifier.size(17.dp)) {
    val palette = LocalFinderMaterials.current.palette
    val resolved = when (color) { Muted -> palette.secondaryInk; Accent -> palette.icon; Ink -> palette.ink; else -> color }
    val folder = if (palette.dark) FinderFolderColors.Dark else FinderFolderColors.Light
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = w * .075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            "folder", "folder-outline" -> {
                val tab = Path().apply {
                    moveTo(w*.06f,h*.27f); quadraticBezierTo(w*.06f,h*.14f,w*.16f,h*.14f)
                    lineTo(w*.38f,h*.14f); lineTo(w*.48f,h*.27f); lineTo(w*.85f,h*.27f)
                    quadraticBezierTo(w*.94f,h*.27f,w*.94f,h*.39f); lineTo(w*.94f,h*.82f)
                    quadraticBezierTo(w*.94f,h*.93f,w*.85f,h*.93f); lineTo(w*.15f,h*.93f)
                    quadraticBezierTo(w*.06f,h*.93f,w*.06f,h*.82f); close()
                }
                if (kind == "folder-outline") drawPath(tab,resolved,style=stroke)
                else {
                    drawRoundRect(Color.Black.copy(alpha = if (palette.dark) .20f else .10f),
                        Offset(w*.085f,h*.385f),androidx.compose.ui.geometry.Size(w*.85f,h*.58f),
                        androidx.compose.ui.geometry.CornerRadius(w*.085f))
                    drawPath(tab, folder.base)
                    drawRoundRect(Brush.verticalGradient(listOf(folder.top, folder.base, folder.bottom)),
                        Offset(w*.06f,h*.31f),androidx.compose.ui.geometry.Size(w*.88f,h*.62f),
                        androidx.compose.ui.geometry.CornerRadius(w*.07f))
                    drawLine(folder.highlight.copy(alpha = .78f),Offset(w*.13f,h*.35f),Offset(w*.87f,h*.35f),w*.015f)
                    drawLine(folder.highlight.copy(alpha = .40f),
                        Offset(w*.13f,h*.84f),Offset(w*.87f,h*.84f),w*.012f)
                }
            }
            "list" -> listOf(.25f,.5f,.75f).forEach { y ->
                drawCircle(resolved, w*.04f, Offset(w*.16f,h*y))
                drawLine(resolved,Offset(w*.32f,h*y),Offset(w*.86f,h*y),w*.065f)
            }
            "trash" -> {
                drawRoundRect(resolved,Offset(w*.2f,h*.30f),androidx.compose.ui.geometry.Size(w*.6f,h*.58f),
                    androidx.compose.ui.geometry.CornerRadius(w*.04f),style=stroke)
                drawLine(resolved,Offset(w*.12f,h*.23f),Offset(w*.88f,h*.23f),w*.08f)
                drawLine(resolved,Offset(w*.35f,h*.42f),Offset(w*.35f,h*.74f),w*.06f)
                drawLine(resolved,Offset(w*.65f,h*.42f),Offset(w*.65f,h*.74f),w*.06f)
            }
            "gallery" -> {
                drawRoundRect(resolved,Offset(w*.1f,h*.15f),androidx.compose.ui.geometry.Size(w*.8f,h*.52f),
                    androidx.compose.ui.geometry.CornerRadius(w*.07f),style=stroke)
                drawLine(resolved,Offset(w*.12f,h*.83f),Offset(w*.88f,h*.83f),w*.06f)
            }
            "icons" -> listOf(.18f,.58f).forEach { x -> listOf(.18f,.58f).forEach { y ->
                drawRoundRect(resolved,Offset(w*x,h*y),androidx.compose.ui.geometry.Size(w*.25f,h*.25f),androidx.compose.ui.geometry.CornerRadius(w*.045f),style=stroke)
            } }
            "back", "forward", "down" -> {
                val p = Path().apply {
                    if (kind == "down") { moveTo(w*.25f,h*.4f); lineTo(w*.5f,h*.65f); lineTo(w*.75f,h*.4f) }
                    else { val a = if (kind == "back") .65f else .35f; val b = 1-a
                        moveTo(w*a,h*.2f); lineTo(w*b,h*.5f); lineTo(w*a,h*.8f) }
                }; drawPath(p, resolved, style = stroke)
            }
            "columns" -> {
                drawRoundRect(resolved, Offset(w*.05f,h*.15f), androidx.compose.ui.geometry.Size(w*.9f,h*.7f), androidx.compose.ui.geometry.CornerRadius(w*.08f), style = stroke)
                listOf(.35f,.65f).forEach { drawLine(resolved,Offset(w*it,h*.17f),Offset(w*it,h*.83f),w*.07f) }
            }
            "search" -> {
                drawCircle(resolved, w*.28f, Offset(w*.4f,h*.4f), style = stroke)
                drawLine(resolved,Offset(w*.62f,h*.62f),Offset(w*.9f,h*.9f),w*.08f)
            }
            "preview" -> {
                drawRect(resolved, Offset(w*.08f,h*.2f), androidx.compose.ui.geometry.Size(w*.84f,h*.6f), style = stroke)
                drawLine(resolved,Offset(w*.62f,h*.2f),Offset(w*.62f,h*.8f),w*.07f)
            }
            "more" -> listOf(.25f,.5f,.75f).forEach { x ->
                drawCircle(resolved, radius = w*.07f, center = Offset(w*x,h*.5f))
            }
            "sort" -> listOf(.25f,.5f,.75f).forEachIndexed { i, y -> drawLine(resolved,Offset(w*.18f,h*y),Offset(w*(.85f-i*.15f),h*y),w*.07f) }
            "home" -> {
                val p = Path().apply { moveTo(w*.12f,h*.46f); lineTo(w*.5f,h*.12f); lineTo(w*.88f,h*.46f)
                    moveTo(w*.25f,h*.4f); lineTo(w*.25f,h*.86f); lineTo(w*.75f,h*.86f); lineTo(w*.75f,h*.4f) }
                drawPath(p,resolved,style=stroke)
            }
            "drive" -> {
                drawRoundRect(resolved,Offset(w*.08f,h*.18f),
                    androidx.compose.ui.geometry.Size(w*.84f,h*.65f),
                    androidx.compose.ui.geometry.CornerRadius(w*.1f),style=stroke)
                drawLine(resolved,Offset(w*.18f,h*.63f),Offset(w*.82f,h*.63f),w*.05f)
                drawCircle(resolved,w*.035f,Offset(w*.74f,h*.74f))
            }

            "download" -> {
                drawLine(resolved,Offset(w*.5f,h*.1f),Offset(w*.5f,h*.64f),w*.072f)
                val p=Path().apply{moveTo(w*.29f,h*.43f);lineTo(w*.5f,h*.65f);lineTo(w*.71f,h*.43f)}
                drawPath(p,resolved,style=stroke)
                drawLine(resolved,Offset(w*.16f,h*.73f),Offset(w*.16f,h*.88f),w*.075f)
                drawLine(resolved,Offset(w*.16f,h*.88f),Offset(w*.84f,h*.88f),w*.075f)
                drawLine(resolved,Offset(w*.84f,h*.88f),Offset(w*.84f,h*.73f),w*.075f)
            }
            "document" -> {
                val p=Path().apply{moveTo(w*.24f,h*.08f);lineTo(w*.59f,h*.08f);
                    lineTo(w*.80f,h*.31f);lineTo(w*.80f,h*.91f);
                    lineTo(w*.24f,h*.91f);close()}
                drawPath(p,resolved,style=stroke)
                listOf(.47f,.60f,.73f).forEach{y->
                    drawLine(resolved,Offset(w*.34f,h*y),Offset(w*.70f,h*y),w*.052f)
                }
            }
            "picture" -> {
                drawRoundRect(resolved,Offset(w*.10f,h*.17f),
                    androidx.compose.ui.geometry.Size(w*.8f,h*.67f),
                    androidx.compose.ui.geometry.CornerRadius(w*.10f),style=stroke)
                drawCircle(resolved,w*.07f,Offset(w*.67f,h*.36f))
                val p=Path().apply{moveTo(w*.16f,h*.74f);lineTo(w*.37f,h*.51f);
                    lineTo(w*.52f,h*.65f);lineTo(w*.65f,h*.54f);lineTo(w*.83f,h*.74f)}
                drawPath(p,resolved,style=stroke)
            }
            "music" -> {
                drawLine(resolved,Offset(w*.45f,h*.25f),Offset(w*.45f,h*.78f),w*.077f)
                drawLine(resolved,Offset(w*.45f,h*.25f),Offset(w*.85f,h*.15f),w*.08f)
                drawLine(resolved,Offset(w*.85f,h*.15f),Offset(w*.85f,h*.68f),w*.08f)
                drawCircle(resolved,w*.13f,Offset(w*.33f,h*.79f))
                drawCircle(resolved,w*.13f,Offset(w*.73f,h*.69f))
            }
            "video" -> {
                drawRoundRect(resolved,Offset(w*.12f,h*.22f),
                    androidx.compose.ui.geometry.Size(w*.76f,h*.62f),
                    androidx.compose.ui.geometry.CornerRadius(w*.08f),style=stroke)
                val p=Path().apply{moveTo(w*.40f,h*.39f);lineTo(w*.67f,h*.53f);
                    lineTo(w*.40f,h*.68f);close()}
                drawPath(p,resolved,style=stroke)
            }

            else -> {
                drawRoundRect(resolved,Offset(w*.2f,h*.1f),androidx.compose.ui.geometry.Size(w*.6f,h*.8f),androidx.compose.ui.geometry.CornerRadius(w*.06f),style=stroke)
                drawLine(resolved,Offset(w*.34f,h*.45f),Offset(w*.66f,h*.45f),w*.05f)
                drawLine(resolved,Offset(w*.34f,h*.62f),Offset(w*.66f,h*.62f),w*.05f)
            }
        }
    }
}

@Composable
fun FinderDesktopHost(viewModel: FileBrowserViewModel, mode: DesktopMode, onMobile: () -> Unit) {
    val session by viewModel.activeSession.collectAsState()
    val browser by viewModel.browseState.collectAsState()
    val operation by viewModel.operationState.collectAsState()
    val conflict by viewModel.transferConflict.collectAsState()
    val uiMessage by viewModel.snackbarMessage.collectAsState()
    val lastOperationResult by viewModel.lastOperationResult.collectAsState()
    val trash by viewModel.trashState.collectAsState()
    val clipboardPaths by viewModel.clipboardPaths.collectAsState()
    val savedConnections by viewModel.connections.collectAsState()
    val context = LocalContext.current
    val rootView = LocalView.current
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
        Column(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Label(if (session?.source != null && session?.source != FileSource.LOCAL)
                "Este proveedor utiliza el navegador de Voyager" else "Abriendo FinderDex…", color = Muted)
            if (session?.source != null && session?.source != FileSource.LOCAL)
                Label("Volver a vista móvil", Modifier.clickable(onClick = onMobile).padding(14.dp), color = Accent)
        }
        return
    }
    val sharedDragState = remember(controller.session.id) { FinderDragDropState() }
    val recentStore = remember(context) { FinderRecentStore(context) }
    val bookmarks by viewModel.bookmarks.collectAsState()
    val favoritePrefs = remember(context) { context.getSharedPreferences("finderdex_favorites", Context.MODE_PRIVATE) }
    var favoriteOrder by remember(favoritePrefs) { mutableStateOf(
        runCatching {
            val json = JSONArray(favoritePrefs.getString("order", "[]"))
            (0 until json.length()).map { json.getString(it) }
        }.getOrDefault(emptyList())
    ) }
    // Only external content:// payloads enter Voyager's existing cancellable upload engine.
    // Permissions remain alive until its onFinished callback, including cancellation.
    DisposableEffect(rootView, controller, sharedDragState) {
        val listener = android.view.View.OnDragListener { _, event ->
            val target = sharedDragState.externalTargetAt(Offset(event.x, event.y))
                ?: controller.currentPath
            val ready = viewModel.browseState.value.currentPath == controller.currentPath &&
                FinderCrossAppTransfer.writableLocalFolder(target) &&
                viewModel.operationState.value !is com.voyagerfiles.viewmodel.OperationState.Running
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED ->
                    ready && FinderCrossAppTransfer.activity(context) != null
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    sharedDragState.updateExternalHover(Offset(event.x, event.y))
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED,
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    sharedDragState.clearExternalHover()
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    sharedDragState.clearExternalHover()
                    val uris = FinderCrossAppTransfer.incomingUris(event)
                    if (!ready || !FinderCrossAppPolicy.accepts(
                            uris.map { it.toString() }, true, false)) false
                    else {
                        val activity = FinderCrossAppTransfer.activity(context)
                        val permission = activity?.requestDragAndDropPermissions(event)
                        if (permission == null) false
                        else {
                            viewModel.uploadDocuments(uris, target) { permission.release() }
                            true
                        }
                    }
                }
                else -> true
            }
        }
        rootView.setOnDragListener(listener)
        onDispose {
            sharedDragState.clearExternalHover()
            rootView.setOnDragListener(null)
        }
    }
    val orderedFavorites = bookmarks.filter { it.source == FileSource.LOCAL }
        .sortedWith(compareBy { favoriteOrder.indexOf(it.path).let { index ->
            if (index < 0) Int.MAX_VALUE else index
        } })
    var volumes by remember(context) { mutableStateOf(emptyList<com.voyagerfiles.util.StorageDirectory>()) }
    LaunchedEffect(context) {
        volumes = withContext(Dispatchers.IO) { FileUtils.getStorageDirectories(context) }
    }
    var message by remember { mutableStateOf<String?>(null) }
    val imagePrefs = remember(context) { context.getSharedPreferences("finderdex_sidebar_images", Context.MODE_PRIVATE) }
    var customImages by remember(imagePrefs) {
        mutableStateOf(imagePrefs.all.mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        }.toMap())
    }
    var imageTarget by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = imageTarget
        if (target != null && uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                imagePrefs.edit().putString(target, uri.toString()).apply()
                customImages = customImages + (target to uri.toString())
            }.onFailure { message = it.localizedMessage ?: "No se pudo conservar el permiso de imagen" }
        }
        imageTarget = null
    }
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            message = it.resolve(context.resources)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(browser.files, controller) {
        if (!browser.isLoading) controller.refresh()
    }
    FinderDesktopScreen(controller, mode, bookmarks = orderedFavorites.map { it.name to it.path },
        volumes = volumes.map { it.name to it.path },
        savedConnections = savedConnections,
        onConnectNetwork = { conn -> viewModel.connectToRemote(conn); onMobile() },
        externalDragState = sharedDragState,
        onRoot = viewModel::openLocalRoot,
        onMobile = onMobile,
        favoriteImages = customImages,
        trashEntries = trash.entries,
        trashLoading = trash.isLoading,
        trashError = trash.error?.resolve(context.resources),
        onRefreshTrash = viewModel::refreshTrash,
        onRestoreTrash = { id ->
            viewModel.clearTrashSelection()
            viewModel.toggleTrashSelection(id)
            viewModel.restoreSelectedTrash()
        },
        onRemoveFavorite = { path -> viewModel.removeBookmark(path, FileSource.LOCAL) },
        onReorderFavorite = { path, delta ->
            val current = orderedFavorites.map { it.path }.toMutableList()
            val index = current.indexOf(path)
            val next = index + delta
            if (index >= 0 && next in current.indices) {
                java.util.Collections.swap(current, index, next)
                favoriteOrder = current
                favoritePrefs.edit().putString("order", JSONArray(current).toString()).apply()
            }
        },
        onPickFavoriteImage = { path -> imageTarget = path; imagePicker.launch(arrayOf("image/*")) },
        onRemoveFavoriteImage = { path ->
            customImages[path]?.let { uri ->
                runCatching { context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            imagePrefs.edit().remove(path).apply()
            customImages = customImages - path
        },
        onOpen = { file -> FileUtils.openFile(context, file)
            .onSuccess { recentStore.record(file) }
            .onFailure { message = it.localizedMessage ?: "No se pudo abrir el archivo" } },
        operationsAvailable = true,
        clipboardAvailable = clipboardPaths.isNotEmpty(),
        onPaste = {
            if (viewModel.browseState.value.currentPath == controller.currentPath) viewModel.paste()
            else message = "Espera a que termine la navegación antes de pegar"
        },
        operationProgress = (operation as? com.voyagerfiles.viewmodel.OperationState.Running)?.progress,
        operationResult = lastOperationResult,
        operationRunning = operation is com.voyagerfiles.viewmodel.OperationState.Running,
        onCopyPaths = { paths, cut ->
            if (cut) viewModel.cutToClipboard(paths, false)
            else viewModel.copyToClipboard(paths, false)
        },
        onRename = { file, name -> viewModel.rename(file.path, name) },
        onCreateFolder = viewModel::createDirectory,
        onMovePaths = viewModel::moveDesktopPaths,
        onCopyPathsTo = viewModel::copyDesktopPaths,
        onDeletePaths = { paths, permanent ->
            viewModel.deletePaths(paths, if (permanent) com.voyagerfiles.viewmodel.DeleteMode.PERMANENT
                else com.voyagerfiles.viewmodel.DeleteMode.TRASH)
        },
        canCancelOperation = (operation as? com.voyagerfiles.viewmodel.OperationState.Running)?.cancellable == true,
        onCancelOperation = viewModel::cancelOperation,
        onFileAction = { action, file ->
            when (action) {
                FinderFileAction.COPY -> viewModel.copyToClipboard(listOf(file.path), false)
                FinderFileAction.CUT -> viewModel.cutToClipboard(listOf(file.path), false)
                FinderFileAction.OPEN_WITH -> FileUtils.openFileWith(context, file)
                    .onFailure { message = it.localizedMessage ?: "No se pudo abrir" }
                FinderFileAction.SHARE -> {
                    val selected = controller.columns.value.lastOrNull()?.selected.orEmpty()
                    val files = if (file.path in selected) browser.files.filter { it.path in selected } else listOf(file)
                    FileUtils.shareFiles(context, files)
                        .onFailure { message = it.localizedMessage ?: "No se pudo compartir" }
                }
                FinderFileAction.PASTE_INTO -> if (file.isDirectory) viewModel.pasteIntoDesktopFolder(file.path)
                FinderFileAction.DUPLICATE -> {
                    val selected = controller.columns.value.lastOrNull()?.selected.orEmpty()
                    val paths = if (file.path in selected) selected.toList() else listOf(file.path)
                    if (viewModel.browseState.value.currentPath == controller.currentPath)
                        viewModel.duplicateDesktopPaths(paths)
                    else message = "Espera a que termine la navegación antes de duplicar"
                }
                FinderFileAction.EXTRACT -> if (file.isArchive) {
                    if (viewModel.browseState.value.currentPath == controller.currentPath)
                        viewModel.extractArchive(file.path)
                    else message = "Espera a que termine la navegación antes de extraer"
                }
                FinderFileAction.ZIP -> {
                    val current = viewModel.browseState.value
                    val selected = controller.columns.value.lastOrNull()?.selected.orEmpty()
                    val selection = if (file.path in selected && selected.size > 1)
                        current.files.filter { it.path in selected }
                    else current.files.filter { it.path == file.path }
                    if (current.currentPath == controller.currentPath &&
                        selection.isNotEmpty() && selection.all { it.path in current.files.map { f -> f.path } }) {
                        val name = com.voyagerfiles.ui.screens.BrowserArchiveActions.defaultZipName(
                            selection, current.files.map { it.name }.toSet(), "Archivo")
                        viewModel.createZip(selection.map { it.path }, name)
                    } else message = "Espera a que termine la navegación antes de comprimir"
                }
                FinderFileAction.FAVORITE -> if (file.isDirectory) viewModel.addBookmark(file.path, file.name)
                FinderFileAction.TRASH -> viewModel.deletePaths(listOf(file.path), com.voyagerfiles.viewmodel.DeleteMode.TRASH)
                FinderFileAction.DELETE -> viewModel.deletePaths(listOf(file.path), com.voyagerfiles.viewmodel.DeleteMode.PERMANENT)
                else -> Unit
            }
        },
        message = message, onDismissMessage = { message = null })
    conflict?.let { request ->
        TransferConflictDialog(request) { response ->
            viewModel.resolveTransferConflict(request, response)
        }
    }
}

@Composable
internal fun FinderDesktopScreen(
    controller: FinderColumnController,
    mode: DesktopMode,
    externalDragState: FinderDragDropState? = null,
    bookmarks: List<Pair<String, String>> = emptyList(),
    volumes: List<Pair<String, String>> = emptyList(),
    savedConnections: List<com.voyagerfiles.data.model.RemoteConnection> = emptyList(),
    onConnectNetwork: (com.voyagerfiles.data.model.RemoteConnection) -> Unit = {},
    favoriteImages: Map<String, String> = emptyMap(),
    trashEntries: List<com.voyagerfiles.data.model.TrashEntry> = emptyList(),
    trashLoading: Boolean = false,
    trashError: String? = null,
    onRefreshTrash: () -> Unit = {},
    onRestoreTrash: (String) -> Unit = {},
    onRemoveFavorite: (String) -> Unit = {},
    onReorderFavorite: (String, Int) -> Unit = { _, _ -> },
    onPickFavoriteImage: (String) -> Unit = {},
    onRemoveFavoriteImage: (String) -> Unit = {},
    onRoot: (String) -> Unit = {},
    onMobile: () -> Unit = {},
    onOpen: (FileItem) -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
    operationsAvailable: Boolean = false,
    clipboardAvailable: Boolean = false,
    onPaste: () -> Unit = {},
    operationProgress: com.voyagerfiles.viewmodel.TransferProgress? = null,
    canCancelOperation: Boolean = false,
    onCancelOperation: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onMovePaths: (List<String>, String) -> Unit = { _, _ -> },
    onCopyPathsTo: (List<String>, String) -> Unit = { _, _ -> },
    operationResult: com.voyagerfiles.viewmodel.OperationResult? = null,
    operationRunning: Boolean = false,
    onDeletePaths: (List<String>, Boolean) -> Unit = { _, _ -> },
    onCopyPaths: (List<String>, Boolean) -> Unit = { _, _ -> },
    onRename: (FileItem, String) -> Unit = { _, _ -> },
    onFileAction: (FinderFileAction, FileItem) -> Unit = { _, _ -> },
) {
    val columns by controller.columns.collectAsState()
    val chrome = rememberFinderWindowChrome()
    val fallbackDragState = remember(controller.session.id) { FinderDragDropState() }
    val dragState = externalDragState ?: fallbackDragState
    val androidView = LocalView.current
    SideEffect {
        dragState.externalError = { /* Unsupported MIME or missing URI access; no source mutation. */ }
        dragState.externalStart = { paths ->
            val indexed = columns.flatMap { it.files }.associateBy { it.path }
            val selectedFiles = paths.mapNotNull { indexed[it] }
            selectedFiles.size == paths.size &&
                runCatching { FinderCrossAppTransfer.start(androidView, selectedFiles) }.getOrDefault(false)
        }
    }
    LaunchedEffect(dragState, operationsAvailable) { dragState.enabled = operationsAvailable }
    LaunchedEffect(controller.session.rootPath) {
        dragState.allowedRoot = controller.session.rootPath
    }
    LaunchedEffect(controller.currentPath) {
        dragState.clearTargets()
        if (dragState.draggingPaths.isEmpty()) {
            dragState.cancel()
            dragState.dismiss()
        }
    }
    LaunchedEffect(operationResult, dragState.awaitingTransfer) {
        if (dragState.awaitingTransfer && operationResult != null && !operationRunning) {
            dragState.completeTransfer(operationResult.outcome ==
                com.voyagerfiles.viewmodel.OperationOutcome.COMPLETED)
        }
    }
    val localContext = LocalContext.current
    val widthPrefs = remember(localContext) {
        localContext.getSharedPreferences("finderdex_window", Context.MODE_PRIVATE)
    }
    var sidebarWidth by rememberSaveable { mutableFloatStateOf(
        widthPrefs.getFloat("sidebarWidth", FinderWindowGeometry.SIDEBAR_DEFAULT_WIDTH_DP)
            .coerceIn(FinderWindowGeometry.SIDEBAR_MIN_WIDTH_DP, FinderWindowGeometry.SIDEBAR_MAX_WIDTH_DP)
    ) }
    LaunchedEffect(sidebarWidth) {
        delay(220)
        widthPrefs.edit().putFloat("sidebarWidth", sidebarWidth).apply()
    }
    var viewStyle by rememberSaveable { mutableStateOf(FinderViewStyle.ICONS) }
    val zoomPrefs = remember(localContext) {
        localContext.getSharedPreferences("finderdex_view_zoom", Context.MODE_PRIVATE)
    }
    val zoomByView = remember(zoomPrefs) {
        mutableStateListOf(*FinderViewStyle.values().map { style ->
            FinderZoomPresets.clamp(style, zoomPrefs.getInt(style.name, 2))
        }.toTypedArray())
    }
    val zoomIndex = zoomByView[viewStyle.ordinal]
    val zoomDensity = FinderZoomPresets.density(viewStyle, zoomIndex)
    LaunchedEffect(zoomByView.toList()) {
        delay(320)
        zoomPrefs.edit().apply {
            FinderViewStyle.values().forEach { style ->
                putInt(style.name, zoomByView[style.ordinal])
            }
        }.apply()
    }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var descending by rememberSaveable { mutableStateOf(false) }
    var sortBy by rememberSaveable { mutableStateOf(SortBy.NAME) }
    var quickLook by remember { mutableStateOf<FileItem?>(null) }
    var infoFile by remember { mutableStateOf<FileItem?>(null) }
    var renameFile by remember { mutableStateOf<FileItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteFile by remember { mutableStateOf<FileItem?>(null) }
    var permanentDelete by remember { mutableStateOf(false) }
    var pendingDeletePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var newFolderDialog by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    fun execute(action: FinderFileAction, file: FileItem) {
        val owningColumn = columns.lastOrNull { col -> col.files.any { it.path == file.path } }
        val selectedPaths = owningColumn?.selected.orEmpty()
        val effectivePaths = if (file.path in selectedPaths) selectedPaths.toList() else listOf(file.path)
        when (action) {
            FinderFileAction.COPY -> onCopyPaths(effectivePaths, false)
            FinderFileAction.CUT -> onCopyPaths(effectivePaths, true)
            FinderFileAction.OPEN -> if (file.isDirectory) {
                columns.indexOfLast { it.files.any { entry -> entry.path == file.path } }
                    .takeIf { it >= 0 }?.let { controller.select(it, file) }
            } else onOpen(file)
            FinderFileAction.QUICK_LOOK -> if (!file.isDirectory) quickLook = file
            FinderFileAction.INFO -> infoFile = file
            FinderFileAction.RENAME -> { renameFile = file; renameText = file.name }
            FinderFileAction.TRASH, FinderFileAction.DELETE -> {
                deleteFile = file; permanentDelete = action == FinderFileAction.DELETE
                pendingDeletePaths = if (file.path in selectedPaths) {
                    owningColumn?.files.orEmpty().filter { it.path in selectedPaths }.map { it.path }
                } else listOf(file.path)
            }
            else -> onFileAction(action, file)
        }
    }
    val density = LocalDensity.current.density
    val materials = rememberFinderMaterials()
    val palette = materials.palette
    val focusTracker = remember { FinderControlFocus() }
    BackHandler(controller.canBack && quickLook == null && infoFile == null && renameFile == null && deleteFile == null) { controller.back() }
    // The normal resizable DeX Activity is not a floating translucent Window.
    // Apply the glass tint to app-owned surfaces, never to native caption controls.
    CompositionLocalProvider(
        LocalFinderMaterials provides materials,
        LocalFinderControlFocus provides focusTracker,
    ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val availableWindowWidth = maxWidth.value
    val shell = FinderWindowGeometry.resolve(
        availableWindowWidth, maxHeight.value, sidebarWidth,
        chrome.layout.trailing / density,
    )
    val effectiveSidebarWidth = shell.sidebarWidthDp
    val dividerWidth = if (shell.sidebarRail) 1.dp else FinderWindowGeometry.DIVIDER_HIT_WIDTH_DP.dp
    val windowShape = RoundedCornerShape(shell.cornerRadiusDp.dp)
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars.exclude(WindowInsets.captionBar))
        .padding(top = (chrome.layout.topInset / density).dp)
        .shadow(FinderWindowGeometry.SHADOW_ELEVATION_DP.dp, windowShape, clip = false)
        .clip(windowShape)
        .background(palette.window)
        .border(FinderWindowGeometry.BORDER_WIDTH_DP.dp, palette.edge, windowShape)
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false
            else when {
                event.key == Key.Escape && searching -> { searching = false; search = ""; true }
                event.key == Key.Escape && dragState.draggingPaths.isNotEmpty() -> { dragState.cancel(); true }
                event.key == Key.Escape && quickLook == null && controller.selectedFile != null -> { controller.clearSelection(); true }
                !searching && !focusTracker.hasFocusedControl && event.key in listOf(Key.DirectionLeft, Key.DirectionRight,
                    Key.DirectionUp, Key.DirectionDown) && !event.isAltPressed -> {
                    val columnDelta = if (viewStyle == FinderViewStyle.ICONS) controller.iconColumns else 1
                    val step = when (event.key) {
                        Key.DirectionLeft -> -1
                        Key.DirectionRight -> 1
                        Key.DirectionUp -> -columnDelta
                        else -> columnDelta
                    }
                    val visibleOrder = finderOrderFiles(
                        columns.lastOrNull()?.files.orEmpty().filter { it.name.contains(search, true) },
                        sortBy, descending).map { it.path }
                    controller.moveSelectionBy(step, event.isShiftPressed, visibleOrder)
                    true
                }
                event.key == Key.Spacebar && !focusTracker.hasFocusedControl && !searching && quickLook == null && controller.selectedFile?.isDirectory == false -> {
                    quickLook = controller.selectedFile; true
                }
                (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.C && operationsAvailable && !focusTracker.hasFocusedControl -> {
                    controller.columns.value.lastOrNull()?.selected.orEmpty().toList()
                        .takeIf { it.isNotEmpty() }?.let { onCopyPaths(it, false) }; true
                }
                (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.X && operationsAvailable && !focusTracker.hasFocusedControl -> {
                    controller.columns.value.lastOrNull()?.selected.orEmpty().toList()
                        .takeIf { it.isNotEmpty() }?.let { onCopyPaths(it, true) }; true
                }
                (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.V && operationsAvailable && !focusTracker.hasFocusedControl -> {
                    onPaste(); true
                }
                (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.A && !searching && !focusTracker.hasFocusedControl -> {
                    controller.selectAll(); true
                }
                event.key == Key.Delete && controller.selectedFile != null && operationsAvailable && !focusTracker.hasFocusedControl -> {
                    val selected = controller.columns.value.lastOrNull()?.selected.orEmpty()
                    pendingDeletePaths = selected.toList()
                    deleteFile = controller.selectedFile
                    permanentDelete = event.isShiftPressed
                    true
                }
                event.isAltPressed && event.key == Key.DirectionLeft && !focusTracker.hasFocusedControl -> { controller.back(); true }
                event.isAltPressed && event.key == Key.DirectionRight && !focusTracker.hasFocusedControl -> { controller.forward(); true }
                event.key == Key.Enter && !focusTracker.hasFocusedControl -> { controller.selectedFile?.let { file ->
                    if (file.isDirectory) controller.select(columns.indexOfLast { file.path in it.selected }, file) else onOpen(file)
                }; true }
                else -> false
            }
        }) {
        FinderToolbar(controller, chrome, effectiveSidebarWidth, dividerWidth.value, availableWindowWidth,
            shell.compactToolbar, shell.minimalToolbar,
            viewStyle, { viewStyle = it }, searching, search, { searching = !searching; if (!searching) search = "" }, { search = it },
            showPreview, { showPreview = !showPreview }, descending, { descending = !descending },
            sortBy, { sortBy = it }, onMobile,
            canCreateFolder = operationsAvailable,
            onNewFolder = { newFolderName = ""; newFolderDialog = true },
            onTrash = { onRefreshTrash(); showTrash = true },
            previewAvailable = shell.showPreview && mode != DesktopMode.COMPACT)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (effectiveSidebarWidth >= 40f) {
                FinderSidebar(controller.currentPath, effectiveSidebarWidth.dp, bookmarks, volumes, onRoot,
                    onAnalytics = { showAnalytics = true }, onRecents = { showRecents = true },
                    connections = savedConnections, onConnection = onConnectNetwork,
                    favoriteImages, onRemoveFavorite, onReorderFavorite, onPickFavoriteImage, onRemoveFavoriteImage,
                    dragState, rail = shell.sidebarRail)
            }
            // The original resize gesture remains authoritative; hover is visual only.
            val resizeInteraction = remember { MutableInteractionSource() }
            val dividerHovered by resizeInteraction.collectIsHoveredAsState()
            Box(Modifier.fillMaxHeight().width(dividerWidth)
                .background(if (dividerHovered && !shell.sidebarRail) palette.hover else palette.sidebar)
                .hoverable(resizeInteraction)
                .pointerHoverIcon(if (shell.sidebarRail) PointerIcon.Default else PointerIcon(android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW))
                .pointerInput(density, shell.sidebarRail, availableWindowWidth) {
                    if (!shell.sidebarRail) detectHorizontalDragGestures { change, delta ->
                        change.consume()
                        val maxWidth = minOf(
                            FinderWindowGeometry.SIDEBAR_MAX_WIDTH_DP,
                            (availableWindowWidth - FinderWindowGeometry.DIVIDER_HIT_WIDTH_DP -
                                FinderWindowGeometry.CONTENT_MIN_WIDTH_DP).coerceAtLeast(
                                    FinderWindowGeometry.SIDEBAR_MIN_WIDTH_DP))
                        sidebarWidth = (sidebarWidth + delta / density).coerceIn(
                            FinderWindowGeometry.SIDEBAR_MIN_WIDTH_DP, maxWidth)
                    }
                }
                .pointerInput(shell.sidebarRail) {
                    if (!shell.sidebarRail) detectTapGestures(
                        onDoubleTap = { sidebarWidth = FinderWindowGeometry.SIDEBAR_DEFAULT_WIDTH_DP })
                }) {
                Box(Modifier.fillMaxHeight().width(1.dp).align(Alignment.CenterEnd).background(palette.separator))
            }
            when (viewStyle) {
                FinderViewStyle.ICONS -> FinderIconView(controller, columns, search, descending, sortBy, Modifier.weight(1f), onOpen, operationsAvailable, clipboardAvailable, ::execute, dragState, zoomDensity)
                FinderViewStyle.LIST -> FinderListView(controller, columns, search, descending, sortBy, Modifier.weight(1f), onOpen, operationsAvailable, clipboardAvailable, ::execute, dragState, zoomDensity)
                FinderViewStyle.COLUMNS -> FinderWorkspace(controller, columns, search, descending, sortBy, Modifier.weight(1f).background(palette.content), onOpen, operationsAvailable, clipboardAvailable, ::execute, dragState, zoomDensity)
                FinderViewStyle.GALLERY -> FinderGalleryView(controller, columns, search, descending, sortBy, Modifier.weight(1f), onOpen, operationsAvailable, clipboardAvailable, ::execute, dragState, zoomDensity)
            }
            if (showPreview && shell.showPreview && mode != DesktopMode.COMPACT) FinderPreview(controller.selectedFile, onOpen)
        }
        if (message != null) Row(Modifier.fillMaxWidth().background(palette.feedback).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(message, Modifier.weight(1f)); Label("Cerrar", Modifier.clickable(onClick = onDismissMessage).padding(4.dp), color = Accent)
        }
        if (operationProgress != null) Row(Modifier.fillMaxWidth().background(palette.progress)
            .padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("Operación en curso · " + (operationProgress.detailText ?: "Procesando…"),
                Modifier.weight(1f), color = Ink)
            operationProgress.percentageText?.let { Label(it, color = Accent, bold = true) }
            if (canCancelOperation) Label("Cancelar",
                Modifier.clickable { onCancelOperation() }.padding(horizontal = 9.dp), color = Accent)
        }
        if (dragState.draggingPaths.isNotEmpty()) Row(
            Modifier.fillMaxWidth().background(palette.dragHint).padding(7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Label(dragState.hoveredDestination?.let { "Soltar para mover a ${it.substringAfterLast('/')}" }
                ?: "Arrastra a una carpeta para mover · Se pedirá confirmación",
                color = Accent)
        }
        // Breadcrumbs now live in the toolbar; the footer is a single status row.
        FinderStatusBar(controller, columns, effectiveSidebarWidth + dividerWidth.value,
            shell.statusBarHeightDp, shell.compactToolbar, viewStyle, zoomIndex) { index ->
            zoomByView[viewStyle.ordinal] = FinderZoomPresets.clamp(viewStyle, index)
        }
    }
    if (dragState.springFolder != null && dragState.draggingPaths.isNotEmpty())
        FinderSpringFolderPanel(dragState, Modifier.align(Alignment.CenterEnd).padding(end = 26.dp))
    FinderDragGhost(dragState)
    FinderSpringFolderTimer(dragState)
    }
    }
    quickLook?.let { file ->
        FinderQuickLookDialog(file, onDismiss = { quickLook = null }, onNavigate = { direction ->
            val ordered = columns.lastOrNull()?.files.orEmpty().filterNot { it.isDirectory }
            val current = ordered.indexOfFirst { it.path == file.path }
            if (ordered.isNotEmpty() && current >= 0)
                quickLook = ordered[(current + direction + ordered.size) % ordered.size]
        })
    }
    infoFile?.let { file ->
        AlertDialog(onDismissRequest = { infoFile = null }, title = { Text(stringResource(R.string.finderdex_info_title)) },
            text = { Column {
                PreviewDetail("Nombre", file.name)
                PreviewDetail("Tipo MIME", file.mimeType)
                PreviewDetail("Tamaño", file.formattedSize)
                PreviewDetail("Ubicación", file.path)
                PreviewDetail("Origen", file.source.name)
                PreviewDetail("Permisos", file.permissions ?: "No disponibles")
                PreviewDetail("Modificado", DateFormat.getDateTimeInstance().format(file.lastModified))
            } },
            confirmButton = { TextButton(onClick = { infoFile = null }) { Text(stringResource(R.string.finderdex_close)) } })
    }
    renameFile?.let { file ->
        AlertDialog(onDismissRequest = { renameFile = null }, title = { Text(stringResource(R.string.dialog_rename)) },
            text = { BasicTextField(renameText, { renameText = it },
                Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(8.dp)).padding(12.dp),
                singleLine = true, textStyle = TextStyle(fontSize = 15.sp, color = Ink)) },
            confirmButton = { TextButton(onClick = {
                if (renameText.isNotBlank() && renameText != file.name) onRename(file, renameText)
                renameFile = null
            }) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = { renameFile = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    deleteFile?.let { _ ->
        AlertDialog(onDismissRequest = { deleteFile = null },
            title = { Text(stringResource(if (permanentDelete) R.string.dialog_delete_permanently else R.string.dialog_move_to_trash)) },
            text = { Text(if (permanentDelete)
                "Eliminar ${pendingDeletePaths.size} elemento(s) definitivamente. No se puede deshacer."
                else "¿Mover ${pendingDeletePaths.size} elemento(s) a la papelera?") },
            confirmButton = { TextButton(onClick = {
                onDeletePaths(pendingDeletePaths.toList(), permanentDelete)
                deleteFile = null
                pendingDeletePaths = emptyList()
            }) { Text(stringResource(if (permanentDelete) R.string.action_delete else R.string.action_move)) } },
            dismissButton = { TextButton(onClick = { deleteFile = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    dragState.pending?.let { drop ->
        AlertDialog(onDismissRequest = { dragState.dismiss() },
            title = { Text(stringResource(if (drop.action == FinderDropAction.COPY) R.string.finderdex_copy_items_title else R.string.finderdex_move_items_title)) },
            text = { Text(stringResource(R.string.finderdex_confirm_drop,
                stringResource(if (drop.action == FinderDropAction.COPY) R.string.action_copy else R.string.action_move),
                drop.paths.size, drop.destination)) },
            confirmButton = { TextButton(enabled = !operationRunning, onClick = {
                dragState.acceptPending()
                if (drop.action == FinderDropAction.COPY) onCopyPathsTo(drop.paths, drop.destination)
                else onMovePaths(drop.paths, drop.destination)
            }) { Text(stringResource(if (drop.action == FinderDropAction.COPY) R.string.action_copy else R.string.action_move)) } },
            dismissButton = { TextButton(onClick = { dragState.dismiss() }) { Text(stringResource(R.string.action_cancel)) } })
    }
    if (showRecents) FinderRecentsDialog(columns.lastOrNull()?.files.orEmpty(),
        onDismiss = { showRecents = false },
        onOpenLocation = { path -> showRecents = false; onRoot(path) })
    if (showAnalytics) FinderAnalyticsDialog(controller.currentPath,
        onDismiss = { showAnalytics = false },
        onOpenFolder = { path -> showAnalytics = false; onRoot(path) })
    if (showTrash) FinderTrashDialog(trashEntries, trashLoading, trashError,
        onRestore = onRestoreTrash, onRefresh = onRefreshTrash, onDismiss = { showTrash = false })
    if (newFolderDialog) AlertDialog(
        onDismissRequest = { newFolderDialog = false }, title = { Text(stringResource(R.string.finderdex_toolbar_new_folder)) },
        text = { BasicTextField(newFolderName, { newFolderName = it },
            Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(8.dp)).padding(12.dp),
            singleLine = true, textStyle = TextStyle(fontSize = 15.sp, color = Ink)) },
        confirmButton = { TextButton(onClick = {
            if (newFolderName.isNotBlank()) onCreateFolder(newFolderName)
            newFolderDialog = false
        }) { Text(stringResource(R.string.action_create)) } },
        dismissButton = { TextButton(onClick = { newFolderDialog = false }) { Text(stringResource(R.string.action_cancel)) } })
}

internal enum class FinderViewStyle { ICONS, LIST, COLUMNS, GALLERY }

/** Original outline glyph mapping; favorites retain their own saved images. */
internal fun finderSidebarGlyph(folder: String): String = when (folder) {
    "Download" -> "download"
    "Documents" -> "document"
    "Pictures" -> "picture"
    "Music" -> "music"
    "Movies" -> "video"
    else -> "folder-outline"
}

@Composable
private fun FinderSidebar(currentPath: String, width: Dp, bookmarks: List<Pair<String, String>>, volumes: List<Pair<String, String>>, onRoot: (String) -> Unit,
    onAnalytics: () -> Unit, onRecents: () -> Unit,
    connections: List<com.voyagerfiles.data.model.RemoteConnection>,
    onConnection: (com.voyagerfiles.data.model.RemoteConnection) -> Unit,
    favoriteImages: Map<String, String>, onRemoveFavorite: (String) -> Unit,
    onReorderFavorite: (String, Int) -> Unit,
    onPickFavoriteImage: (String) -> Unit, onRemoveFavoriteImage: (String) -> Unit,
    dragState: FinderDragDropState, rail: Boolean = false) {
    val root = Environment.getExternalStorageDirectory().path
    val glass = LocalFinderMaterials.current.palette
    val sidebarScroll = rememberScrollState()
    var showDriveStatus by remember { mutableStateOf(false) }
    var showOneDriveStatus by remember { mutableStateOf(false) }
    Box(Modifier.width(width).fillMaxHeight()
        // Light layered glass, without blur on file content or extra pointer interceptors.
        .shadow(3.dp, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp), clip = false)
        .background(Brush.verticalGradient(listOf(glass.titlebar, glass.sidebar, glass.sidebar)))
        .border(.6.dp, glass.illuminatedEdge, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))) {
    Column(Modifier.fillMaxSize().verticalScroll(sidebarScroll)
        .padding(horizontal = if (rail) 8.dp else 12.dp, vertical = 14.dp)) {
        var favoritesExpanded by remember { mutableStateOf(true) }
        var locationsExpanded by remember { mutableStateOf(true) }
        if (!rail) SidebarHeading("Favoritos", favoritesExpanded) { favoritesExpanded = !favoritesExpanded }
        if (favoritesExpanded || rail) {
            SidebarItem(stringResource(R.string.finderdex_recents_title), "recent:local", "document",
                false, { onRecents() }, dragState = null, rail = rail)
            SidebarItem("Inicio", root, "home", currentPath == root, onRoot, dragState = dragState, rail = rail)
            listOf("Descargas" to "Download", "Documentos" to "Documents", "Imágenes" to "Pictures", "Música" to "Music", "Películas" to "Movies").forEach { (name, folder) ->
                val path = java.io.File(root, folder).path
                if (java.io.File(path).isDirectory && java.io.File(path).canRead())
                    SidebarItem(name, path, finderSidebarGlyph(folder), currentPath == path || currentPath.startsWith("$path/"), onRoot,
                        dragState = dragState, rail = rail)
            }
            bookmarks.forEach { (name, path) ->
                if (java.io.File(path).isDirectory && java.io.File(path).canRead())
                    SidebarItem(name, path, "folder-outline", currentPath == path, onRoot,
                        pinned = true, imageUri = favoriteImages[path],
                        onRemoveFavorite = onRemoveFavorite,
                        onReorderFavorite = onReorderFavorite,
                        onPickImage = onPickFavoriteImage, onClearImage = onRemoveFavoriteImage,
                        dragState = dragState, rail = rail)
            }
        }
        Spacer(Modifier.height(20.dp))
        if (!rail) SidebarHeading("Ubicaciones", locationsExpanded) { locationsExpanded = !locationsExpanded }
        if (locationsExpanded || rail) {
            SidebarItem(stringResource(R.string.finderdex_analytics_title), "analytics:storage", "drive",
                false, { onAnalytics() }, dragState = null, rail = rail)
            volumes.forEach { (name, path) ->
                if (java.io.File(path).isDirectory && java.io.File(path).canRead())
                    SidebarItem(name, path, "drive", path != root && currentPath == path, onRoot, dragState = dragState, rail = rail)
            }
            // Connection is intentionally unavailable until user-authorized OAuth is configured.
            // Never route a cloud item into the local filesystem or accept local folder drops here.
            SidebarItem(stringResource(R.string.finderdex_cloud_drive_disconnected), "cloud:google-drive", "drive",
                false, { showDriveStatus = true }, dragState = null, rail = rail)
            SidebarItem(stringResource(R.string.finderdex_onedrive_disconnected), "cloud:onedrive", "drive",
                false, { showOneDriveStatus = true }, dragState = null, rail = rail)
            connections.forEach { connection ->
                SidebarItem(connection.name, "network:${connection.id}", "drive",
                    false, { onConnection(connection) }, dragState = null, rail = rail)
            }
        }
    }
    FinderVerticalScrollbar(sidebarScroll, Modifier.align(Alignment.CenterEnd))
    }
    if (showDriveStatus) AlertDialog(
        onDismissRequest = { showDriveStatus = false },
        title = { Text(stringResource(R.string.finderdex_cloud_drive_title)) },
        text = { Text(stringResource(R.string.finderdex_cloud_drive_unavailable)) },
        confirmButton = { TextButton(onClick = { showDriveStatus = false }) { Text(stringResource(R.string.finderdex_cloud_drive_ok)) } },
    )
    if (showOneDriveStatus) AlertDialog(
        onDismissRequest = { showOneDriveStatus = false },
        title = { Text(stringResource(R.string.finderdex_onedrive_title)) },
        text = { Text(stringResource(R.string.finderdex_onedrive_unavailable)) },
        confirmButton = { TextButton(onClick = { showOneDriveStatus = false }) { Text(stringResource(R.string.finderdex_cloud_drive_ok)) } },
    )
}

@Composable
private fun SidebarHeading(text: String, expanded: Boolean, onClick: () -> Unit) {
    val glass = LocalFinderMaterials.current.palette
    var focused by remember { mutableStateOf(false) }
    val focusTracker = LocalFinderControlFocus.current
    Row(Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand).onFocusChanged {
        focused = it.isFocused
        focusTracker.update("section:$text", it.isFocused)
    }.clickable(onClick = onClick).finderFocusOutline(focused).padding(7.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Label(text, Modifier.weight(1f), Muted, 12, true)
        FinderGlyph(if (expanded) "down" else "forward", modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun SidebarItem(name: String, path: String, glyph: String, selected: Boolean, onRoot: (String) -> Unit,
    pinned: Boolean = false, imageUri: String? = null, onRemoveFavorite: (String) -> Unit = {},
    onReorderFavorite: (String, Int) -> Unit = { _, _ -> },
    onPickImage: (String) -> Unit = {}, onClearImage: (String) -> Unit = {},
    dragState: FinderDragDropState? = null, rail: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val focusTracker = LocalFinderControlFocus.current
    val material = LocalFinderMaterials.current
    val palette = material.palette
    val selection = finderAnimatedMaterial(when {
        pressed -> palette.pressed
        selected -> palette.selection(false, material.windowFocused)
        hovered -> palette.hover
        else -> Color.Transparent
    }, selected)
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(11.dp))
        .background(selection)
        .border(.7.dp, if (selected) palette.accent.copy(alpha = .28f) else Color.Transparent,
            RoundedCornerShape(11.dp))
        .hoverable(interaction).pointerHoverIcon(PointerIcon.Hand)
        .then(if (dragState != null) Modifier.finderFolderDropTarget("sidebar:$path", path, dragState) else Modifier)
        .then(if (dragState != null) Modifier.finderDragVisual(path, true, dragState) else Modifier)
        .finderRightClick { if (pinned) menu = true }
        .onFocusChanged { focused = it.isFocused; focusTracker.update("sidebar:$path", it.isFocused) }
        .clickable(interactionSource = interaction, indication = null) { onRoot(path) }
        .finderFocusOutline(focused)
        .semantics { contentDescription = name }
        .padding(horizontal = if (rail) 0.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (rail) Arrangement.Center else Arrangement.Start) {
        if (imageUri != null) AsyncImage(model = Uri.parse(imageUri), contentDescription = null,
            modifier = Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)), contentScale = ContentScale.Crop)
        else FinderGlyph(glyph, palette.icon, Modifier.size(20.dp))
        if (!rail) {
            Spacer(Modifier.width(8.dp))
            Label(name, Modifier.weight(1f),
                color = if (selected) palette.selectionText(false, material.windowFocused) else palette.ink,
                size = 13, bold = selected)
        }
        if (pinned) FinderGlassMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.finderdex_favorite_up)) },
                onClick = { menu = false; onReorderFavorite(path, -1) })
            DropdownMenuItem(text = { Text(stringResource(R.string.finderdex_favorite_down)) },
                onClick = { menu = false; onReorderFavorite(path, 1) })
            DropdownMenuItem(text = { Text(stringResource(R.string.finderdex_favorite_pick_icon)) },
                onClick = { menu = false; onPickImage(path) })
            if (imageUri != null) DropdownMenuItem(text = { Text(stringResource(R.string.finderdex_favorite_reset_icon)) },
                onClick = { menu = false; onClearImage(path) })
            DropdownMenuItem(text = { Text(stringResource(R.string.finderdex_favorite_remove)) },
                onClick = { menu = false; onRemoveFavorite(path) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FinderIconView(controller: FinderColumnController, columns: List<FinderColumn>, query: String,
    descending: Boolean, sortBy: SortBy, modifier: Modifier, onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean, clipboardAvailable: Boolean, onFileAction: (FinderFileAction, FileItem) -> Unit,
    dragState: FinderDragDropState, zoom: FinderViewDensity) {
    val column = columns.lastOrNull()
    val files = remember(column?.files, query, descending, sortBy) {
        finderOrderFiles(column?.files.orEmpty().filter { it.name.contains(query, true) }, sortBy, descending)
    }
    val marquee = remember(column?.path) { FinderMarqueeState() }
    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { item -> item.column }?.plus(1) ?: 1 }
            .collect { controller.iconColumns = it.coerceAtLeast(1) }
    }
    var gridBounds by remember { mutableStateOf(Rect.Zero) }
    FinderEdgeAutoScroll(marquee, dragState, gridBounds) { gridState.scrollBy(it) }
    val glass = LocalFinderMaterials.current
    val colors = glass.palette
    Box(modifier.fillMaxHeight().background(colors.content).testTag("finder-icons")
        .finderMarqueeGesture(controller, marquee, dragState)) {
        key(column?.path) {
            LazyVerticalGrid(columns = GridCells.Adaptive(zoom.gridMinimumDp.dp),
                state = gridState, modifier = Modifier.fillMaxSize().onGloballyPositioned { gridBounds = it.boundsInRoot() },
                contentPadding = PaddingValues(
                    horizontal = FinderContentTokens.ICON_GRID_OUTER_HORIZONTAL_DP.dp,
                    vertical = FinderContentTokens.ICON_GRID_OUTER_VERTICAL_DP.dp),
                horizontalArrangement = Arrangement.spacedBy(FinderContentTokens.ICON_GRID_COLUMN_GAP_DP.dp),
                verticalArrangement = Arrangement.spacedBy(FinderContentTokens.ICON_GRID_ROW_GAP_DP.dp)) {
                items(files, key = { it.path }) { file ->
                    val selected = file.path in column?.selected.orEmpty()
                    val interaction = remember { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val pressed by interaction.collectIsPressedAsState()
                    var menuOpen by remember { mutableStateOf(false) }
                    val secondaryClick = remember(file.path) { FinderSecondaryClickGate() }
                    var ctrl by remember { mutableStateOf(false) }
                    var shift by remember { mutableStateOf(false) }
                    var alt by remember { mutableStateOf(false) }
                    var keyboardFocused by remember { mutableStateOf(false) }
                    val rowColor = finderAnimatedMaterial(
                        if (pressed) colors.pressed
                        else if (selected) colors.selection(false, glass.windowFocused)
                        else Color.Transparent, selected)
                    Column(Modifier.fillMaxWidth().height(zoom.cellHeightDp.dp)
                        .clip(RoundedCornerShape(FinderContentTokens.ICON_CELL_CORNER_DP.dp))
                        .background(rowColor)
                        .border(.7.dp, if (selected) colors.accent.copy(alpha = .52f) else Color.Transparent,
                            RoundedCornerShape(FinderContentTokens.ICON_CELL_CORNER_DP.dp))
                        .hoverable(interaction)
                        .onFocusChanged { keyboardFocused = it.isFocused }
                        .finderMarqueeItem(file.path, marquee)
                        .finderDragVisual(file.path, file.isDirectory, dragState)
                        .finderDragSource(file.path, column?.selected.orEmpty(), dragState, ctrl, alt)
                        .then(if (file.isDirectory) Modifier.finderFolderDropTarget("icon:${file.path}", file.path, dragState) else Modifier)
                        .finderMouseSelectionModifiers { c, sh, a -> ctrl = c; shift = sh; alt = a }
                        .finderRightClick(secondaryClick) {
                            controller.selectForContextMenu(columns.lastIndex, file)
                            menuOpen = true
                        }
                        .combinedClickable(interactionSource = interaction, indication = null,
                            onClick = { if (secondaryClick.allowsPrimaryClick())
                                controller.selectOnly(columns.lastIndex, file, ctrl, shift, files.map { it.path }) },
                            onDoubleClick = { if (secondaryClick.allowsPrimaryClick()) {
                                if (file.isDirectory) controller.select(columns.lastIndex, file) else onOpen(file)
                            } },
                            onLongClick = { controller.selectForContextMenu(columns.lastIndex, file); menuOpen = true })
                        .finderFocusOutline(keyboardFocused)
                        .semantics { this.selected = selected }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size((zoom.iconDp + 16f).dp, (zoom.iconDp + 10f).dp)
                            .finderIconHover(hovered)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.selected.copy(alpha = .55f) else Color.Transparent),
                            contentAlignment = Alignment.Center) {
                            if (file.isDirectory) FinderGlyph("folder",
                                modifier = Modifier.size(zoom.iconDp.dp))
                            else {
                                FileThumbnailOrIcon(file, (zoom.iconDp * .94f).dp)
                                Box(Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp)) {
                                    FinderFileFormatBadge(file)
                                }
                            }
                        }
                        Spacer(Modifier.height(1.dp))
                        BasicText(file.name, Modifier.clip(RoundedCornerShape(FinderContentTokens.ICON_LABEL_CORNER_DP.dp))
                            .background(if (selected) colors.selection(false, glass.windowFocused) else Color.Transparent).padding(4.dp, 2.dp),
                            style = TextStyle(color = if (selected) colors.selectionText(false, glass.windowFocused) else colors.ink, fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center, lineHeight = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FinderFileMenu(menuOpen, { menuOpen = false }, file.isDirectory, operationsAvailable,
                            if (file.path in column?.selected.orEmpty()) column?.selected?.size ?: 1 else 1, isArchive = file.isArchive, canPaste = clipboardAvailable) {
                            onFileAction(it, file)
                        }
                    }
                }
            }
        }
        if (column?.loading == true) Label("Cargando…", Modifier.align(Alignment.Center), Muted)
        else if (column?.error != null) Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(column.error, style = TextStyle(color = Muted, fontSize = 12.sp))
            Label("Reintentar", Modifier.clickable { controller.refresh() }.padding(12.dp), Accent)
        } else if (files.isEmpty()) Label(if (query.isBlank()) "Carpeta vacía" else "Sin resultados", Modifier.align(Alignment.Center), Muted)
        FinderMarqueeOverlay(marquee)
        FinderLazyGridScrollbar(gridState, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun FinderWorkspace(controller: FinderColumnController, columns: List<FinderColumn>, query: String, descending: Boolean, sortBy: SortBy, modifier: Modifier, onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean, clipboardAvailable: Boolean, onFileAction: (FinderFileAction, FileItem) -> Unit,
    dragState: FinderDragDropState, zoom: FinderViewDensity) {
    val horizontal = rememberScrollState()
    LaunchedEffect(columns.size) { horizontal.animateScrollTo(horizontal.maxValue) }
    Row(modifier.fillMaxHeight().testTag("finder-columns").horizontalScroll(horizontal)) {
        columns.forEachIndexed { index, column ->
            key(column.id) {
                ColumnView(controller, index, column, index == columns.indexOfLast { it.selected.isNotEmpty() }, if (index == columns.lastIndex) query else "", descending, sortBy, onOpen,
                    operationsAvailable && index == columns.lastIndex, clipboardAvailable, onFileAction, dragState, zoom)
            }
        }
        Spacer(Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnView(controller: FinderColumnController, index: Int, column: FinderColumn, active: Boolean, query: String, descending: Boolean, sortBy: SortBy, onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean, clipboardAvailable: Boolean, onFileAction: (FinderFileAction, FileItem) -> Unit,
    dragState: FinderDragDropState, zoom: FinderViewDensity) {
    val scroll = rememberLazyListState(column.scrollIndex, column.scrollOffset)
    val marqueeForEdge = remember(column.id) { FinderMarqueeState() }
    var columnBounds by remember { mutableStateOf(Rect.Zero) }
    FinderEdgeAutoScroll(marqueeForEdge, dragState, columnBounds) { scroll.scrollBy(it) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.firstVisibleItemIndex to scroll.firstVisibleItemScrollOffset }.collect { (i, offset) -> controller.rememberScroll(index, i, offset) }
    }
    val glass = LocalFinderMaterials.current
    val palette = glass.palette
    Row(Modifier.width(FinderContentTokens.COLUMN_WIDTH_DP.dp).fillMaxHeight()) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val filtered = remember(column.files, query, descending, sortBy) {
                finderOrderFiles(column.files.filter { it.name.contains(query, true) }, sortBy, descending)
            }
            LazyColumn(state = scroll, modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { columnBounds = it.boundsInRoot() }.padding(horizontal = 5.dp), contentPadding = PaddingValues(vertical = 7.dp)) {
                itemsIndexed(filtered, key = { _, f -> f.path }) { _, file ->
                    val selected = file.path in column.selected
                    val interaction = remember { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val pressed by interaction.collectIsPressedAsState()
                    var menuOpen by remember { mutableStateOf(false) }
                    val secondaryClick = remember(file.path) { FinderSecondaryClickGate() }
                    var ctrl by remember { mutableStateOf(false) }
                    var shift by remember { mutableStateOf(false) }
                    var alt by remember { mutableStateOf(false) }
                    var keyboardFocused by remember { mutableStateOf(false) }
                    val rowColor = finderAnimatedMaterial(
                        if (pressed) palette.pressed
                        else if (selected) palette.selection(active, glass.windowFocused)
                        else Color.Transparent, selected)
                    val rowInk = if (selected) palette.selectionText(active, glass.windowFocused) else palette.ink
                    Row(Modifier.fillMaxWidth().height(zoom.rowHeightDp.dp)
                        .clip(RoundedCornerShape(FinderContentTokens.COLUMN_ROW_CORNER_DP.dp))
                        .background(rowColor).hoverable(interaction)
                        .onFocusChanged { keyboardFocused = it.isFocused }
                        .finderDragVisual(file.path, file.isDirectory, dragState)
                        .finderDragSource(file.path, column.selected, dragState, ctrl, alt)
                        .then(if (file.isDirectory) Modifier.finderFolderDropTarget("column:${index}:${file.path}", file.path, dragState) else Modifier)
                        .finderMouseSelectionModifiers { c, sh, a -> ctrl = c; shift = sh; alt = a }
                        .finderRightClick(secondaryClick) {
                            controller.selectForContextMenu(index, file)
                            menuOpen = true
                        }
                        .combinedClickable(interactionSource = interaction, indication = null,
                            onClick = { if (secondaryClick.allowsPrimaryClick()) {
                                if (index == controller.columns.value.lastIndex && (ctrl || shift))
                                    controller.selectOnly(index, file, ctrl, shift, filtered.map { it.path })
                                else controller.select(index, file)
                            } },
                            onDoubleClick = { if (secondaryClick.allowsPrimaryClick()) {
                                if (file.isDirectory) controller.select(index, file) else onOpen(file)
                            } },
                            onLongClick = { controller.selectForContextMenu(index, file); menuOpen = true })
                        .finderFocusOutline(keyboardFocused)
                        .semantics { this.selected = selected }.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size((zoom.iconDp + 2f).dp).finderIconHover(hovered),
                            contentAlignment = Alignment.Center) {
                            if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(zoom.iconDp.dp))
                            else FileThumbnailOrIcon(file, zoom.iconDp.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Label(file.name, Modifier.weight(1f), rowInk)
                        if (file.isDirectory) FinderGlyph("forward", rowInk, Modifier.size(10.dp))
                        FinderFileMenu(menuOpen, { menuOpen = false }, file.isDirectory, operationsAvailable,
                            if (file.path in column.selected) column.selected.size else 1, isArchive = file.isArchive, canPaste = clipboardAvailable) {
                            onFileAction(it, file)
                        }
                    }
                }
            }
            if (column.loading) Label("Cargando…", Modifier.align(Alignment.Center), Muted)
            else if (column.error != null) Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(column.error, style = TextStyle(color = Muted, fontSize = 12.sp))
                Label("Reintentar", Modifier.clickable { controller.refresh() }.padding(10.dp), Accent)
            } else if (filtered.isEmpty()) Label(if (query.isEmpty()) "Carpeta vacía" else "Sin resultados", Modifier.align(Alignment.Center), Muted)
            FinderLazyListScrollbar(scroll, Modifier.align(Alignment.CenterEnd))
        }
        Box(Modifier.width(0.5.dp).fillMaxHeight().background(palette.separator))
    }
}


@Composable
private fun FinderPreview(file: FileItem?, onOpen: (FileItem) -> Unit) {
    val palette = LocalFinderMaterials.current.palette
    val scroll = rememberScrollState()
    Row(Modifier.width(FinderWindowGeometry.PREVIEW_WIDTH_DP.dp)
        .fillMaxHeight().background(palette.footer)) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(palette.separator))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)
                .padding(FinderContentTokens.PREVIEW_PADDING_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Label("VISTA PREVIA", Modifier.fillMaxWidth(), color = Muted, size = 11, bold = true)
                Spacer(Modifier.height(24.dp))
                if (file == null) {
                    Label("Selecciona un archivo", color = Muted, size = 12)
                } else {
                    Box(Modifier.size(FinderContentTokens.PREVIEW_FRAME_WIDTH_DP.dp,
                        FinderContentTokens.PREVIEW_FRAME_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.content)
                        .border(0.7.dp, palette.separator, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(FinderContentTokens.PREVIEW_FOLDER_DP.dp))
                        else FileThumbnailOrIcon(file, FinderContentTokens.PREVIEW_THUMBNAIL_DP.dp)
                    }
                    Spacer(Modifier.height(18.dp))
                    Label(file.name, Modifier.fillMaxWidth(), bold = true, size = 13)
                    Spacer(Modifier.height(5.dp))
                    Label(if (file.isDirectory) "Carpeta" else file.formattedSize,
                        color = Muted, size = 12)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.separator))
                    Spacer(Modifier.height(12.dp))
                    PreviewDetail("Nombre", file.name)
                    PreviewDetail("Clase",
                        if (file.isDirectory) "Carpeta" else file.extension.uppercase().ifEmpty { "Archivo" })
                    PreviewDetail("Modificado",
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(file.lastModified))
                    if (!file.isDirectory) {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.clip(RoundedCornerShape(16.dp))
                            .background(palette.selected)
                            .clickable { onOpen(file) }
                            .padding(horizontal = 20.dp, vertical = 9.dp)) {
                            Label("Abrir", color = palette.selectedText, bold = true)
                        }
                    }
                }
            }
            FinderVerticalScrollbar(scroll, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun PreviewDetail(title: String, value: String) {
    val palette = LocalFinderMaterials.current.palette
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Label(title, color = Muted, size = 11)
        Spacer(Modifier.height(4.dp))
        BasicText(value, style = TextStyle(color = palette.ink, fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

