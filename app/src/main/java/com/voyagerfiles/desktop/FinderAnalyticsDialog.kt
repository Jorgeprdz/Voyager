package com.voyagerfiles.desktop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.collectLatest

/**
 * Scanner is the existing StorageCategoryIndex, NOT an additional filesystem walker.
 * Only scanned files contribute to displayed totals; no extrapolation, deletion or network I/O.
 */
@Composable
internal fun FinderAnalyticsDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    val context = LocalContext.current
    val volumes = remember(context) { FileUtils.getStorageVolumes(context) }
    val indexer = remember { StorageCategoryIndex() }
    var snapshot by remember { mutableStateOf(CategoryIndexState()) }
    var duplicateScan by remember { mutableStateOf<DuplicateScan?>(null) }
    var duplicateError by remember { mutableStateOf(false) }
    var duplicateRunning by remember { mutableStateOf(false) }
    var duplicateRequest by remember { mutableIntStateOf(0) }
    val hasPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
    var scanRequest by remember { mutableIntStateOf(0) }
    var scanJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var duplicateJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(volumes, scanRequest) {
        scanJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        indexer.scanAll(volumes, showHidden = false, hasPermission = hasPermission)
            .collectLatest { state -> snapshot = state }
    }
    LaunchedEffect(duplicateRequest) {
        if (duplicateRequest == 0) return@LaunchedEffect
        duplicateJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        duplicateRunning = true
        duplicateScan = null
        duplicateError = false
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
    val capacities = remember(volumes) { volumes.mapNotNull { volume ->
        volume.path?.takeIf { volume.isAvailable }?.let { volume.description to FileUtils.getStorageInfo(it) }
    } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_analytics_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())) {
                capacities.forEach { (name, space) ->
                    Text(name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.home_storage_usage, FileItem.formatFileSize(space.used), FileItem.formatFileSize(space.total)))
                    LinearProgressIndicator(progress = { space.usedPercentage.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = com.voyagerfiles.ui.theme.FinderMobileTokens.folderBlue)
                    Text("${FileItem.formatFileSize(space.available)} ${stringResource(R.string.home_storage_available)}")
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(snapshot.status.labelRes))
                if (snapshot.status in listOf(CategoryScanStatus.SCANNING, CategoryScanStatus.PENDING)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick = { scanJob?.cancel(); snapshot = snapshot.copy(status = CategoryScanStatus.CANCELLED) }) { Text(stringResource(R.string.action_cancel)) }
                } else TextButton(onClick = { scanRequest++ }) { Text(stringResource(R.string.r31_retry)) }
                Text(stringResource(R.string.category_scope))
                val skipped = snapshot.coverage.sumOf { it.skippedEntries + it.inaccessibleEntries }
                if (skipped > 0) Text(stringResource(R.string.finderdex_analytics_skipped, skipped))
                if (report.scannedFiles > 0) {
                    Text(stringResource(R.string.finderdex_analytics_summary,
                        report.scannedFiles, FileItem.formatFileSize(report.scannedBytes)))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.finderdex_analytics_categories))
                    report.categories.forEach { item ->
                        val title = when (item.category) {
                            "video" -> R.string.filter_videos
                            "image" -> R.string.filter_images
                            "audio" -> R.string.filter_audio
                            "document" -> R.string.filter_documents
                            "archive" -> R.string.filter_archives
                            "app" -> R.string.filter_apps
                            else -> R.string.filter_all
                        }
                        Text(stringResource(R.string.finderdex_analytics_entry,
                            stringResource(title), FileItem.formatFileSize(item.bytes)))
                        LinearProgressIndicator(progress = { if (report.scannedBytes > 0) (item.bytes.toFloat() / report.scannedBytes).coerceIn(0f, 1f) else 0f },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = com.voyagerfiles.ui.theme.FinderMobileTokens.folderBlue)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.finderdex_analytics_largest))
                    report.largestFiles.forEach { item ->
                        TextButton(onClick = {
                            onOpenFolder(item.path.substringBeforeLast('/', "/"))
                        }) {
                            Text(stringResource(R.string.finderdex_analytics_entry,
                                item.name, FileItem.formatFileSize(item.size)))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.finderdex_analytics_folders))
                    report.largestFolders.forEach { item ->
                        TextButton(onClick = { onOpenFolder(item.path) }) {
                            Text(stringResource(R.string.finderdex_analytics_entry,
                                item.path.substringAfterLast('/'), FileItem.formatFileSize(item.bytes)))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(enabled = !duplicateRunning && hasPermission(),
                    onClick = { duplicateRequest++ }) {
                    Text(stringResource(if (duplicateRunning) R.string.duplicates_scanning
                        else R.string.duplicates_scan))
                }
                if (duplicateRunning) TextButton(onClick = { duplicateJob?.cancel() }) { Text(stringResource(R.string.action_cancel)) }
                duplicateScan?.let { result ->
                    Text(stringResource(R.string.duplicates_summary,
                        result.groups.size, result.groups.sumOf { it.files.size }))
                    result.groups.take(12).forEach { group ->
                        group.files.take(5).forEach { example ->
                            TextButton(onClick = {
                                onOpenFolder(example.path.substringBeforeLast('/', "/"))
                            }) {
                                Text(example.path.substringAfterLast('/'))
                            }
                        }
                    }
                    if (result.limited) Text(stringResource(R.string.duplicates_limited))
                }
                if (duplicateError) Text(stringResource(R.string.category_failed))
                Text(stringResource(R.string.finderdex_analytics_nondeleting))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.finderdex_close))
            }
        },
    )
}
