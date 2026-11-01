package com.voyagerfiles.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voyagerfiles.R
import com.voyagerfiles.data.duplicates.DuplicateScan
import com.voyagerfiles.data.duplicates.DuplicateScanner
import com.voyagerfiles.data.index.CategoryIndexState
import com.voyagerfiles.data.index.CategoryScanStatus
import com.voyagerfiles.data.index.StorageAnalyticsSummary
import com.voyagerfiles.data.index.StorageCategoryIndex
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.util.FileUtils
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collectLatest

/**
 * Mobile presentation for the existing bounded, permission-aware analytics engine.
 * Capacity is measured from device volumes; category bars are relative ONLY to scanned bytes.
 * No new filesystem walker, no deletion or permissions changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinderMobileAnalyticsPanel(
    currentPath: String,
    onDismiss: () -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    val context = LocalContext.current
    val volumes = remember(context) { FileUtils.getStorageVolumes(context) }
    val indexer = remember { StorageCategoryIndex() }
    var snapshot by remember { mutableStateOf(CategoryIndexState()) }
    var request by remember { mutableIntStateOf(0) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var duplicateJob by remember { mutableStateOf<Job?>(null) }
    var duplicateRequest by remember { mutableIntStateOf(0) }
    var duplicateRunning by remember { mutableStateOf(false) }
    var duplicateScan by remember { mutableStateOf<DuplicateScan?>(null) }
    var duplicateError by remember { mutableStateOf(false) }
    val hasPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(volumes, request) {
        scanJob = currentCoroutineContext()[Job]
        indexer.scanAll(volumes, showHidden = false, hasPermission = hasPermission)
            .collectLatest { snapshot = it }
    }
    LaunchedEffect(duplicateRequest) {
        if (duplicateRequest == 0) return@LaunchedEffect
        duplicateJob = currentCoroutineContext()[Job]
        duplicateRunning = true
        duplicateError = false
        duplicateScan = null
        try {
            duplicateScan = DuplicateScanner().scan(File(currentPath))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            duplicateError = true
        } finally {
            duplicateRunning = false
        }
    }
    val report = remember(snapshot.files) { StorageAnalyticsSummary.fromFiles(snapshot.files, limit = 12) }
    val capacities = remember(volumes) {
        volumes.mapNotNull { volume ->
            volume.path?.takeIf { volume.isAvailable }?.let { volume.description to FileUtils.getStorageInfo(it) }
        }
    }
    val scanning = snapshot.status == CategoryScanStatus.SCANNING || snapshot.status == CategoryScanStatus.PENDING
    val skipped = snapshot.coverage.sumOf { it.skippedEntries + it.inaccessibleEntries }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.content_desc_back))
                }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(stringResource(R.string.finderdex_analytics_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(snapshot.status.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { request++ }, enabled = !scanning) {
                    Icon(Icons.Filled.Refresh, stringResource(R.string.content_desc_refresh))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item("capacity") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        capacities.forEach { (name, space) ->
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(
                                R.string.home_storage_usage,
                                FileItem.formatFileSize(space.used),
                                FileItem.formatFileSize(space.total),
                            ), style = MaterialTheme.typography.bodyMedium)
                            LinearProgressIndicator(
                                progress = { space.usedPercentage.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                            Text(
                                FileItem.formatFileSize(space.available) + " " +
                                    stringResource(R.string.home_storage_available),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item("coverage") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(stringResource(R.string.finderdex_analytics_summary,
                            report.scannedFiles, FileItem.formatFileSize(report.scannedBytes)),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(stringResource(R.string.category_scope),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (skipped > 0) {
                            Text(stringResource(R.string.finderdex_analytics_skipped, skipped),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (scanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            TextButton(onClick = {
                                scanJob?.cancel()
                                snapshot = snapshot.copy(status = CategoryScanStatus.CANCELLED)
                            }) { Text(stringResource(R.string.action_cancel)) }
                        } else if (snapshot.status != CategoryScanStatus.COMPLETE) {
                            TextButton(onClick = { request++ }) { Text(stringResource(R.string.r31_retry)) }
                        }
                    }
                }
                if (report.scannedFiles > 0) {
                    item("categories-title") {
                        Text(stringResource(R.string.finderdex_analytics_categories),
                            style = MaterialTheme.typography.titleSmall)
                    }
                    items(report.categories, key = { "category:" + it.category }) { item ->
                        val title = when (item.category) {
                            "video" -> R.string.filter_videos
                            "image" -> R.string.filter_images
                            "audio" -> R.string.filter_audio
                            "document" -> R.string.filter_documents
                            "archive" -> R.string.filter_archives
                            "app" -> R.string.filter_apps
                            else -> R.string.r321_other
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(title), style = MaterialTheme.typography.bodyMedium)
                                Text(FileItem.formatFileSize(item.bytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LinearProgressIndicator(
                                progress = { if (report.scannedBytes > 0L)
                                    (item.bytes.toFloat() / report.scannedBytes).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        }
                    }
                    item("largest-title") {
                        Text(stringResource(R.string.finderdex_analytics_largest),
                            style = MaterialTheme.typography.titleSmall)
                    }
                    items(report.largestFiles, key = { "file:" + it.path }) { file ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable { onOpenFolder(file.path.substringBeforeLast('/', "/")) },
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(file.name, modifier = Modifier.weight(1f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(12.dp))
                            Text(FileItem.formatFileSize(file.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (report.largestFolders.isNotEmpty()) {
                        item("folders-title") {
                            Text(stringResource(R.string.finderdex_analytics_folders),
                                style = MaterialTheme.typography.titleSmall)
                        }
                        items(report.largestFolders, key = { "folder:" + it.path }) { folder ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable { onOpenFolder(folder.path) },
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(folder.path.substringAfterLast('/'), modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(12.dp))
                                Text(FileItem.formatFileSize(folder.bytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item("duplicates") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TextButton(
                            enabled = !duplicateRunning && hasPermission(),
                            onClick = { duplicateRequest++ },
                        ) {
                            Text(stringResource(
                                if (duplicateRunning) R.string.duplicates_scanning else R.string.duplicates_scan
                            ))
                        }
                        if (duplicateRunning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            TextButton(onClick = { duplicateJob?.cancel() }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                        duplicateScan?.let { result ->
                            Text(stringResource(R.string.duplicates_summary,
                                result.groups.size, result.groups.sumOf { it.files.size }))
                            result.groups.take(12).forEach { group ->
                                group.files.take(5).forEach { example ->
                                    TextButton(onClick = {
                                        onOpenFolder(example.path.substringBeforeLast('/', "/"))
                                    }) { Text(example.path.substringAfterLast('/')) }
                                }
                            }
                            if (result.limited) Text(stringResource(R.string.duplicates_limited))
                        }
                        if (duplicateError) Text(stringResource(R.string.category_failed),
                            color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.finderdex_analytics_nondeleting),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
