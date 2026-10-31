package com.voyagerfiles.desktop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    LaunchedEffect(volumes) {
        indexer.scanAll(volumes, showHidden = false, hasPermission = hasPermission)
            .collectLatest { state -> snapshot = state }
    }
    LaunchedEffect(duplicateRequest) {
        if (duplicateRequest == 0) return@LaunchedEffect
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

    val report = remember(snapshot.status, snapshot.files) {
        if (snapshot.status in setOf(CategoryScanStatus.COMPLETE,
                CategoryScanStatus.PARTIAL, CategoryScanStatus.LIMITED)) {
            StorageAnalyticsSummary.fromFiles(snapshot.files, limit = 12)
        } else null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finderdex_analytics_title)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())) {
                Text(stringResource(snapshot.status.labelRes))
                Text(stringResource(R.string.category_scope))
                val skipped = snapshot.coverage.sumOf { it.skippedEntries + it.inaccessibleEntries }
                if (skipped > 0) Text(stringResource(R.string.finderdex_analytics_skipped, skipped))
                if (report != null) {
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
                duplicateScan?.let { result ->
                    Text(stringResource(R.string.duplicates_summary,
                        result.groups.size, result.groups.sumOf { it.files.size }))
                    result.groups.take(12).forEach { group ->
                        group.files.firstOrNull()?.let { example ->
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
