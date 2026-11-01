package com.voyagerfiles.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.voyagerfiles.ui.theme.FinderMobileTokens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.data.remote.cloud.*
import java.io.IOException
import kotlinx.coroutines.*

/** Same authorized browser in mobile and DeX; remote IDs never enter local navigation. */
@Composable
internal fun CloudBrowserDialog(cloud: CloudService, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember(cloud) { CloudOAuth(context, cloud) }
    val provider = remember(auth) {
        if (cloud == CloudService.GOOGLE_DRIVE) GoogleDriveProvider(OneDriveTokenProvider { auth.token() })
        else OneDriveGraphProvider(OneDriveTokenProvider { auth.token() })
    }
    var authorized by remember { mutableStateOf(auth.authorized) }
    var entries by remember { mutableStateOf(emptyList<CloudEntry>()) }
    var trail by remember { mutableStateOf(listOf("root" to cloud.name)) }
    var next by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var bytes by remember { mutableLongStateOf(0) }
    var download by remember { mutableStateOf<CloudEntry?>(null) }
    var rename by remember { mutableStateOf<CloudEntry?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var trash by remember { mutableStateOf<CloudEntry?>(null) }
    var destination by remember { mutableStateOf<Pair<CloudEntry, Boolean>?>(null) }
    fun launch(block: suspend () -> Unit) {
        if (busy) return
        job = scope.launch {
            busy = true; error = null; bytes = 0
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = context.getString(R.string.r31_cloud_error) }
            finally { busy = false }
        }
    }
    suspend fun load(append: Boolean = false) {
        val page = if (query.isBlank()) provider.list(trail.last().first, if (append) next else null)
            else provider.search(query, if (append) next else null)
        val result = page.getOrThrow()
        entries = (if (append) entries + result.entries else result.entries).distinctBy { it.id }
        next = result.nextToken
    }
    val oauthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.let { intent -> launch { auth.complete(intent); authorized = auth.authorized; load() } }
    }
    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) launch {
            withContext(Dispatchers.IO) {
                var display = "upload"
                var size: Long? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val n = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val s = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (n >= 0) display = cursor.getString(n)
                        if (s >= 0 && !cursor.isNull(s)) size = cursor.getLong(s)
                    }
                }
                val running = currentCoroutineContext()
                context.contentResolver.openInputStream(uri)?.use { source ->
                    provider.upload(trail.last().first, display,
                        context.contentResolver.getType(uri) ?: "application/octet-stream", source, size,
                        CloudCancellation { !running.isActive }) { bytes = it }.getOrThrow()
                } ?: throw IOException("Source unavailable")
            }
            load()
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val item = download
        if (uri != null && item != null) launch {
            try {
                withContext(Dispatchers.IO) {
                    val running = currentCoroutineContext()
                    context.contentResolver.openOutputStream(uri)?.use { target ->
                        provider.download(item.id, target, CloudCancellation { !running.isActive }) { bytes = it }.getOrThrow()
                    } ?: throw IOException("Destination unavailable")
                }
            } catch (failure: Throwable) {
                withContext(NonCancellable + Dispatchers.IO) {
                    runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
                }
                throw failure
            }
        }
    }
    LaunchedEffect(auth) { if (authorized) launch { load() } }
    DisposableEffect(auth) { onDispose { auth.dispose() } }
    Dialog(onDismissRequest = { job?.cancel(); onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (cloud == CloudService.GOOGLE_DRIVE)
                            R.string.finderdex_cloud_drive_title else R.string.finderdex_onedrive_title),
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().heightIn(max = if (authorized) 465.dp else 180.dp)) {
                if (!authorized) {
                    Text(
                        stringResource(if (auth.configured) R.string.r31_cloud_authorize else R.string.r31_cloud_config),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (auth.configured) {
                        Button(enabled = !busy, onClick = {
                            runCatching { oauthLauncher.launch(auth.authorizationIntent()) }
                                .onFailure { error = context.getString(R.string.r31_cloud_error) }
                        }) { Text(stringResource(R.string.r31_connect)) }
                    }
                } else {
                    Row {
                        TextButton(enabled = !busy && trail.size > 1, onClick = {
                            trail = trail.dropLast(1); query = ""; launch { load() }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back)) }
                        Text(trail.last().second, Modifier.weight(1f).padding(12.dp), maxLines = 1)
                        TextButton(enabled = !busy, onClick = {
                            job?.cancel(); auth.disconnect(); authorized = false; entries = emptyList()
                            scope.launch { provider.disconnect() }; onDismiss()
                        }) { Text(stringResource(R.string.r31_disconnect)) }
                    }
                    FinderMobileSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.action_search),
                        clearDescription = stringResource(R.string.content_desc_clear_search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row {
                        TextButton(enabled = !busy, onClick = { launch { load() } }) { Text(stringResource(R.string.action_search)) }
                        TextButton(enabled = !busy && query.isBlank(), onClick = { newFolder = true; name = "" }) { Text(stringResource(R.string.create_new_folder)) }
                        TextButton(enabled = !busy, onClick = { uploadLauncher.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.r31_upload)) }
                    }
                    destination?.let { transfer ->
                        Text(transfer.first.name)
                        Row {
                            TextButton(enabled = !busy, onClick = { launch {
                                if (transfer.second) provider.move(transfer.first.id, trail.last().first).getOrThrow()
                                else provider.copy(transfer.first.id, trail.last().first).getOrThrow()
                                destination = null; load()
                            } }) { Text(stringResource(R.string.r31_use_folder)) }
                            TextButton(onClick = { destination = null }) { Text(stringResource(R.string.action_cancel)) }
                        }
                    }
                    LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 290.dp)) {
                        items(entries, key = { it.id }) { item ->
                            var menu by remember { mutableStateOf(false) }
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(enabled = !busy) {
                                if (item.isFolder) { trail = trail + (item.id to item.name); query = ""; launch { load() } }
                                else { download = item; saveLauncher.launch(item.name + if (item.isNativeDocument) { if (CloudDocumentPolicy.exportMime(item.mimeType) == "text/csv") ".csv" else ".pdf" } else "") }
                            }) {
                                Row(Modifier.weight(1f).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (item.isFolder) FinderMobileFolder(Modifier.size(30.dp))
                                    else Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Box {
                                    TextButton(enabled = !busy, onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.content_desc_more)) }
                                    DropdownMenu(menu, { menu = false }) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.action_rename)) }, onClick = { menu = false; rename = item; name = item.name })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.action_move)) }, onClick = { menu = false; destination = item to true; query = "" })
                                        if (!item.isFolder) DropdownMenuItem(text = { Text(stringResource(R.string.action_copy)) }, onClick = { menu = false; destination = item to false; query = "" })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.home_trash)) }, onClick = { menu = false; trash = item })
                                    }
                                }
                            }
                        }
                    }
                    if (entries.isEmpty() && !busy && error == null) Text(stringResource(R.string.r31_empty))
                    if (next != null) TextButton(enabled = !busy, onClick = { launch { load(true) } }) { Text(stringResource(R.string.r31_more)) }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (bytes > 0) Text(com.voyagerfiles.data.model.FileItem.formatFileSize(bytes))
                    TextButton(onClick = { job?.cancel() }) { Text(stringResource(R.string.action_cancel)) }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (authorized) TextButton(enabled = !busy, onClick = { launch { load() } }) { Text(stringResource(R.string.r31_retry)) }
                }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { job?.cancel(); onDismiss() }) {
                        Text(stringResource(R.string.finderdex_close))
                    }
                }
            }
        }
    }
    if (rename != null || newFolder) AlertDialog(onDismissRequest = { rename = null; newFolder = false },
        title = { Text(stringResource(if (newFolder) R.string.create_new_folder else R.string.action_rename)) },
        text = { OutlinedTextField(name, { name = it }, singleLine = true,
            shape = MaterialTheme.shapes.medium, colors = finderMobileInputColors()) },
        confirmButton = { TextButton(enabled = name.isNotBlank() && '/' !in name && '\\' !in name, onClick = {
            val target = rename; val folder = newFolder; val value = name
            rename = null; newFolder = false
            launch { if (folder) provider.createFolder(trail.last().first, value).getOrThrow()
                else provider.rename(target!!.id, value).getOrThrow(); load() }
        }) { Text(stringResource(R.string.action_rename)) } },
        dismissButton = { TextButton(onClick = { rename = null; newFolder = false }) { Text(stringResource(R.string.action_cancel)) } })
    trash?.let { item -> AlertDialog(onDismissRequest = { trash = null },
        title = { Text(stringResource(R.string.home_trash)) }, text = { Text(item.name) },
        confirmButton = { TextButton(onClick = { trash = null; launch { provider.trash(item.id).getOrThrow(); load() } }) { Text(stringResource(R.string.home_trash)) } },
        dismissButton = { TextButton(onClick = { trash = null }) { Text(stringResource(R.string.action_cancel)) } }) }
}
