package com.voyagerfiles.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.ui.theme.FinderMobileTokens
import com.voyagerfiles.ui.components.TranslationNotice
import com.voyagerfiles.ui.components.rememberTranslationLinkOpener
import com.voyagerfiles.data.index.StorageCategory
import com.voyagerfiles.data.model.Bookmark
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.data.model.HomeSection
import com.voyagerfiles.util.FileUtils
import com.voyagerfiles.util.StorageInfo
import com.voyagerfiles.util.StorageVolumeInfo
import com.voyagerfiles.viewmodel.BrowserSession
import com.voyagerfiles.viewmodel.FileBrowserViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: FileBrowserViewModel,
    onNavigateToBrowser: (String) -> Unit,
    onNavigateToSession: (String, String) -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenSafTree: (Uri) -> Unit,
    hasAllFilesAccess: Boolean,
    onRequestAllFilesAccess: () -> Unit,
    onNavigateToCategory: (StorageCategory) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
) {
    val context = LocalContext.current
    val bookmarks by viewModel.bookmarks.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    var bookmarkToRemove by remember { mutableStateOf<Bookmark?>(null) }
    var cloud by remember { mutableStateOf<com.voyagerfiles.data.remote.cloud.CloudService?>(null) }
    var analytics by remember { mutableStateOf(false) }
    var recents by remember { mutableStateOf(false) }
    var transfers by remember { mutableStateOf(false) }
    val volumes = remember(context, hasAllFilesAccess) {
        if (hasAllFilesAccess) FileUtils.getStorageVolumes(context) else emptyList()
    }
    val primary = volumes.firstOrNull { it.isPrimary } ?: volumes.firstOrNull()
    val directories = remember { FileUtils.getCommonDirectories() }
    val root = android.os.Environment.getExternalStorageDirectory().path
    val saf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            onOpenSafTree(uri)
        }
    }
    Scaffold(containerColor = FinderMobileTokens.background,
        topBar = {
            TopAppBar(title = { Column {
                Text("FinderDex", color = FinderMobileTokens.navy, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.r31_your_files), color = FinderMobileTokens.secondaryInk,
                    style = MaterialTheme.typography.labelMedium)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = FinderMobileTokens.background),
                actions = { IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Filled.Settings, stringResource(R.string.content_desc_settings), tint = FinderMobileTokens.secondaryInk)
                } })
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item("search") {
                androidx.compose.material3.Surface(onClick = onNavigateToSearch,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), color = FinderMobileTokens.ice,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = FinderMobileTokens.folderBlue)
                        Text(stringResource(R.string.search_storage_title), Modifier.padding(start = 12.dp), color = FinderMobileTokens.secondaryInk)
                    }
                }
            }
            item("primary-storage") {
                if (hasAllFilesAccess && primary != null) {
                    val info = primary.path?.let { FileUtils.getStorageInfo(it) }
                    androidx.compose.material3.Surface(onClick = { primary.path?.let(onNavigateToBrowser) },
                        shape = FinderMobileTokens.cardShape, color = FinderMobileTokens.navy,
                        shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.voyagerfiles.ui.components.FinderMobileFolder(Modifier.size(46.dp))
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(primary.description, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                                    if (info != null) Text(stringResource(R.string.home_storage_usage,
                                        FileItem.formatFileSize(info.used), FileItem.formatFileSize(info.total)),
                                        color = FinderMobileTokens.ice, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("›", color = androidx.compose.ui.graphics.Color.White)
                            }
                            if (info != null && info.total > 0) {
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(progress = { (info.used.toFloat() / info.total).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(5.dp), color = androidx.compose.ui.graphics.Color(0xFF85D4FF),
                                    trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .15f))
                                Text("${FileItem.formatFileSize(info.available)} ${stringResource(R.string.home_storage_available)}",
                                    Modifier.padding(top = 8.dp), color = FinderMobileTokens.ice, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else MobileHomeRow(stringResource(R.string.home_limited_storage_title),
                    Icons.Filled.PhoneAndroid, onRequestAllFilesAccess)
            }
            item("locations-title") { MobileHomeHeading(stringResource(R.string.home_folders_title)) }
            if (hasAllFilesAccess) items(directories, key = { "dir:${it.path}" }) { directory ->
                MobileHomeRow(stringResource(directory.labelRes), Icons.Filled.Folder,
                    { onNavigateToBrowser(directory.path) }, folder = true)
            }
            items(volumes.filter { it != primary }, key = { "volume:${it.path}:${it.description}" }) { volume ->
                MobileHomeRow(volume.description, Icons.Filled.SdStorage, { volume.path?.let(onNavigateToBrowser) })
            }
            item("saf") { MobileHomeRow(stringResource(R.string.home_storage_access_framework), Icons.Filled.FolderOpen, { saf.launch(null) }) }
            val localBookmarks = bookmarks.filter { it.source == FileSource.LOCAL }
            if (hasAllFilesAccess && localBookmarks.isNotEmpty()) {
                item("favorites-title") { MobileHomeHeading(stringResource(R.string.home_bookmarks_title)) }
                items(localBookmarks, key = { "bookmark:${it.id}" }) { bookmark ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("home-bookmark-${bookmark.id}")
                        .combinedClickable(onClick = { onNavigateToBrowser(bookmark.path) }, onLongClick = { bookmarkToRemove = bookmark }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmark, null, tint = FinderMobileTokens.folderBlue, modifier = Modifier.size(22.dp))
                        Text(bookmark.name, Modifier.weight(1f).padding(start = 14.dp), color = FinderMobileTokens.navy)
                        IconButton(onClick = { bookmarkToRemove = bookmark }) { Text("⋮") }
                    }
                }
            }
            item("recents") { MobileHomeRow(stringResource(R.string.finderdex_recents_title), Icons.AutoMirrored.Filled.InsertDriveFile, { recents = true }) }
            val visibleSessions = sessions.filter { it.source != FileSource.LOCAL || hasAllFilesAccess }
            if (visibleSessions.isNotEmpty()) {
                item("sessions-title") { MobileHomeHeading(stringResource(R.string.home_active_sessions_title)) }
                items(visibleSessions, key = { "session:${it.id}" }) { session ->
                    MobileHomeRow(session.title, Icons.Filled.FolderOpen, { onNavigateToSession(session.id, session.currentPath) })
                }
            }
            item("tools-title") { MobileHomeHeading(stringResource(R.string.r31_tools)) }
            item("tools") {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageCategory.entries.forEach { category ->
                        androidx.compose.material3.AssistChip(onClick = { onNavigateToCategory(category) },
                            label = { Text(stringResource(category.filter.labelRes)) })
                    }
                }
            }
            item("analyzer") { MobileHomeRow(stringResource(R.string.finderdex_analytics_title), Icons.Filled.SdStorage, { analytics = true }) }
            item("transfers") { MobileHomeRow(stringResource(R.string.transfer_channel), Icons.Filled.FolderOpen, { transfers = true }) }
            item("trash") { MobileHomeRow(stringResource(R.string.home_trash), Icons.Filled.DeleteOutline, onNavigateToTrash) }
            item("network") { MobileHomeRow(stringResource(R.string.home_remote_connections_title), Icons.Filled.Cloud, onNavigateToConnections) }
            item("cloud-title") { MobileHomeHeading(stringResource(R.string.r31_clouds)) }
            item("drive") { MobileHomeRow("Google Drive", Icons.Filled.Cloud, { cloud = com.voyagerfiles.data.remote.cloud.CloudService.GOOGLE_DRIVE }, subtitle = stringResource(R.string.r31_cloud_open)) }
            item("onedrive") { MobileHomeRow("OneDrive", Icons.Filled.Cloud, { cloud = com.voyagerfiles.data.remote.cloud.CloudService.ONEDRIVE }, subtitle = stringResource(R.string.r31_cloud_open)) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (analytics) com.voyagerfiles.desktop.FinderAnalyticsDialog(root, { analytics = false }) { analytics = false; onNavigateToBrowser(it) }
    if (recents) com.voyagerfiles.desktop.FinderRecentsDialog(emptyList(), { recents = false }) { recents = false; onNavigateToBrowser(it) }
    if (transfers) com.voyagerfiles.ui.components.TransferCenterDialog(viewModel) { transfers = false }
    cloud?.let { com.voyagerfiles.ui.components.CloudBrowserDialog(it) { cloud = null } }
    bookmarkToRemove?.let { bookmark ->
        AlertDialog(onDismissRequest = { bookmarkToRemove = null }, title = { Text(stringResource(R.string.action_remove_bookmark)) },
            text = { Text(bookmark.name) }, confirmButton = { TextButton(onClick = {
                viewModel.removeBookmark(bookmark.path, bookmark.source); bookmarkToRemove = null
            }) { Text(stringResource(R.string.action_remove_bookmark)) } },
            dismissButton = { TextButton(onClick = { bookmarkToRemove = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
}

@Composable
private fun MobileHomeHeading(title: String) {
    Text(title, Modifier.padding(top = 18.dp, bottom = 6.dp), style = MaterialTheme.typography.titleSmall, color = FinderMobileTokens.secondaryInk)
}

@Composable
private fun MobileHomeRow(title: String, icon: ImageVector, onClick: () -> Unit, folder: Boolean = false, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (folder) com.voyagerfiles.ui.components.FinderMobileFolder(Modifier.size(34.dp))
        else Icon(icon, null, Modifier.size(28.dp), tint = FinderMobileTokens.folderBlue)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FinderMobileTokens.navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = FinderMobileTokens.secondaryInk)
        }
        Text("›", color = FinderMobileTokens.secondaryInk)
    }
}

@Composable
private fun SafAccessCard(
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.home_document_tree),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.home_storage_access_framework),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun StorageVolumeCard(
    volume: StorageVolumeInfo,
    storageInfo: StorageInfo?,
    onClick: () -> Unit,
) {
    val statusLabel = volume.statusLabelRes?.let { stringResource(it) }
    val isInternal = volume.isPrimary
    val availableLabel = stringResource(R.string.home_storage_available)
    val storageUsage = if (storageInfo != null) {
        stringResource(
            R.string.home_storage_usage,
            FileItem.formatFileSize(storageInfo.used),
            FileItem.formatFileSize(storageInfo.total),
        )
    } else {
        null
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage-volume:${volume.description}")
            .semantics { stateDescription = statusLabel ?: availableLabel }
            .clickable(enabled = volume.isAvailable, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isInternal)
                FinderMobileTokens.ice
            else FinderMobileTokens.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isInternal) Icons.Filled.PhoneAndroid else Icons.Filled.SdStorage,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = FinderMobileTokens.folderBlue,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        volume.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = FinderMobileTokens.navy,
                    )
                    Text(
                        statusLabel ?: storageUsage ?: volume.path.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = FinderMobileTokens.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (storageInfo != null && statusLabel == null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { storageInfo.usedPercentage },
                    modifier = Modifier.fillMaxWidth(),
                    color = FinderMobileTokens.folderBlue,
                    trackColor = FinderMobileTokens.edge,
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionRow(
    session: BrowserSession,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (session.source == FileSource.LOCAL || session.source == FileSource.SAF || session.source == FileSource.ROOT || session.source == FileSource.SHIZUKU) {
                    Icons.Filled.Folder
                } else {
                    Icons.Filled.Cloud
                },
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    session.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isActive) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.home_session_active),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.heightIn(min = 94.dp).clickable(onClick = onClick),
        shape = FinderMobileTokens.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = FinderMobileTokens.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = FinderMobileTokens.folderBlue,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = FinderMobileTokens.navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
