package com.voyagerfiles.app

import android.net.Uri
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.voyagerfiles.R
import com.voyagerfiles.data.archive.ArchiveFormat
import com.voyagerfiles.data.archive.ArchiveProgress
import com.voyagerfiles.data.archive.ExternalArchiveIntent
import com.voyagerfiles.data.archive.ExternalArchiveListing
import com.voyagerfiles.data.archive.ExternalArchiveListingReader
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.remote.saf.SafFileProvider
import com.voyagerfiles.data.repository.FileProvider
import com.voyagerfiles.ui.theme.AppTheme
import com.voyagerfiles.ui.theme.VoyagerTheme
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Exported only for narrow archive ACTION_VIEW MIME types. The source grant is used in
 * this Activity, not converted to a filesystem path or silently retained.
 */
class ExternalArchiveActivity : ComponentActivity() {
    private var sourceUri: Uri? = null
    private var sourceName: String = ""
    private var sourceSize: Long = 0L
    private var previewLoading by mutableStateOf(true)
    private var listing by mutableStateOf<ExternalArchiveListing?>(null)
    private var message by mutableStateOf<String?>(null)
    private var extracting by mutableStateOf(false)
    private var progress by mutableStateOf<ArchiveProgress?>(null)
    private var finished by mutableStateOf(false)
    private var extractionJob: Job? = null

    private val destinationPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) extractInto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receivedIntent = intent
        val source = when (receivedIntent?.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    receivedIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
            }
            else -> receivedIntent?.data
        }
        val mime = receivedIntent?.type ?: source?.let { contentResolver.getType(it) }
        if (source == null || !ExternalArchiveIntent.isSupportedUri(source.scheme, mime)) {
            finish()
            return
        }
        val name = ExternalArchiveIntent.archiveName(documentName(source), mime)
        if (name == null) {
            finish()
            return
        }
        sourceUri = source
        sourceName = name
        sourceSize = documentSize(source)
        setContent {
            val appViewModel = (application as VoyagerApp).browserViewModel
            val appearance by appViewModel.finderAppearance.collectAsState()
            val appTheme by appViewModel.theme.collectAsState()
            VoyagerTheme(appTheme = appTheme, appearance = appearance) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(sourceName, style = MaterialTheme.typography.titleLarge)
                        Text(getString(R.string.external_archive_contents),
                            style = MaterialTheme.typography.titleSmall)
                        when {
                            previewLoading -> {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text(getString(R.string.external_archive_loading))
                            }
                            message != null && listing == null -> {
                                Text(checkNotNull(message), color = MaterialTheme.colorScheme.error)
                            }
                            listing != null -> {
                                val view = checkNotNull(listing)
                                Column(
                                    Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 360.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    view.entries.forEach { entry ->
                                        Text((if (entry.isDirectory) "▸ " else "• ") + entry.path,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (!view.complete) Text(getString(R.string.external_archive_partial))
                            }
                        }
                        if (extracting) {
                            val p = progress
                            val fraction = p?.totalBytes?.takeIf { it > 0 }?.let {
                                (p.processedBytes.toFloat() / it.toFloat()).coerceIn(0f, 1f)
                            }
                            if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            Text(getString(R.string.progress_extracting))
                        }
                        if (message != null && listing != null) {
                            Text(checkNotNull(message))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                extractionJob?.cancel()
                                if (!extracting) finish()
                            }) {
                                Text(getString(if (extracting) R.string.action_cancel else R.string.finderdex_close))
                            }
                            if (!finished && !extracting && listing != null) {
                                Button(onClick = { destinationPicker.launch(null) }) {
                                    Text(getString(R.string.external_archive_choose_destination))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (ArchiveFormat.detect(name)?.canExtract == false) {
            previewLoading = false
            message = getString(R.string.browser_rar_extraction_unavailable)
            return
        }
        lifecycleScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(source)
                        ?: throw FileNotFoundException(getString(R.string.external_archive_missing))
                }
                listing = ExternalArchiveListingReader.read(name, opened, cacheDir)
                message = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = getString(R.string.external_archive_failed, error.message ?: error.javaClass.simpleName)
            } finally {
                previewLoading = false
            }
        }
    }

    private fun extractInto(destination: Uri) {
        val source = sourceUri ?: return
        if (extracting || listing == null) return
        extracting = true
        message = null
        progress = null
        extractionJob = lifecycleScope.launch {
            try {
                val provider = IncomingArchiveProvider(this@ExternalArchiveActivity, destination, source)
                val archive = FileItem(sourceName, source.toString(), false, size = sourceSize)
                val result = com.voyagerfiles.data.archive.ArchiveService.extract(
                    provider, archive, provider.rootPath,
                    onProgress = { p -> runOnUiThread { progress = p } },
                )
                result.fold(
                    onSuccess = { extracted ->
                        finished = true
                        message = getString(R.string.archive_extracted_to, extracted.name)
                    },
                    onFailure = { error ->
                        message = getString(R.string.external_archive_failed,
                            error.message ?: error.javaClass.simpleName)
                    },
                )
            } catch (cancelled: CancellationException) {
                message = getString(R.string.archive_extraction_cancelled)
            } catch (error: Exception) {
                message = getString(R.string.external_archive_failed,
                    error.message ?: error.javaClass.simpleName)
            } finally {
                extracting = false
                progress = null
            }
        }
    }

    private fun documentName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private fun documentSize(uri: Uri): Long = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index)
            else 0L
        } ?: 0L
    }.getOrDefault(0L).coerceAtLeast(0L)
}

private class IncomingArchiveProvider(
    private val context: android.content.Context,
    treeUri: Uri,
    private val sourceUri: Uri,
) : FileProvider {
    private val destination = SafFileProvider(context, treeUri)
    private val entries = AtomicInteger()
    private val writtenBytes = AtomicLong()
    val rootPath: String get() = destination.rootPath
    override suspend fun listFiles(path: String) = destination.listFiles(path)
    override suspend fun createDirectory(path: String, name: String): Result<FileItem> =
        if (entries.incrementAndGet() > MAX_ENTRIES)
            Result.failure(IOException("Extraction exceeds 25,000 entries"))
        else destination.createDirectory(path, name)
    override suspend fun createFile(path: String, name: String): Result<FileItem> =
        if (entries.incrementAndGet() > MAX_ENTRIES)
            Result.failure(IOException("Extraction exceeds 25,000 entries"))
        else destination.createFile(path, name)
    override suspend fun delete(path: String) = destination.delete(path)
    override suspend fun rename(oldPath: String, newName: String) = destination.rename(oldPath, newName)
    override suspend fun copy(sourcePath: String, destPath: String) = destination.copy(sourcePath, destPath)
    override suspend fun move(sourcePath: String, destPath: String) = destination.move(sourcePath, destPath)
    override suspend fun getInputStream(path: String): Result<InputStream> =
        if (path == sourceUri.toString()) withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(sourceUri)
                    ?: throw FileNotFoundException("External archive access expired")
            }
        } else destination.getInputStream(path)
    override suspend fun getOutputStream(path: String): Result<OutputStream> =
        destination.getOutputStream(path).map { target ->
            object : FilterOutputStream(target) {
                private fun budget(amount: Int) {
                    val total = writtenBytes.addAndGet(amount.toLong())
                    if (total > MAX_EXTRACTED_BYTES) {
                        throw IOException("Extraction exceeds the 2 GiB safety budget")
                    }
                }
                override fun write(b: Int) {
                    budget(1)
                    out.write(b)
                }
                override fun write(bytes: ByteArray, off: Int, len: Int) {
                    if (len > 0) budget(len)
                    out.write(bytes, off, len)
                }
            }
        }
    override suspend fun exists(path: String) = destination.exists(path)
    override suspend fun getFileInfo(path: String) = destination.getFileInfo(path)
    override fun getParentPath(path: String) = destination.getParentPath(path)
    override fun ignoresNameCase(path: String) = destination.ignoresNameCase(path)
    private companion object {
        const val MAX_ENTRIES = 25_000
        const val MAX_EXTRACTED_BYTES = 2L * 1024 * 1024 * 1024
    }
}
