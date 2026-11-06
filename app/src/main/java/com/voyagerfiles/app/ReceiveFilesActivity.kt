package com.voyagerfiles.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.ui.components.TransferConflictDialog
import com.voyagerfiles.ui.theme.VoyagerTheme
import com.voyagerfiles.viewmodel.OperationState
import com.voyagerfiles.viewmodel.OperationOutcome

/** Receives temporary content grants; copying uses the existing foreground transfer engine. */
class ReceiveFilesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = SharedFileIntents.incoming(intent)
        val model = (application as VoyagerApp).browserViewModel
        setContent {
            val theme by model.theme.collectAsState()
            val appearance by model.finderAppearance.collectAsState()
            val operation by model.operationState.collectAsState()
            val history by model.transferHistory.collectAsState()
            val conflict by model.transferConflict.collectAsState()
            var operationId by rememberSaveable { mutableStateOf<Long?>(null) }
            var launchFailed by rememberSaveable { mutableStateOf(false) }
            val started = operationId != null
            val ownOperation = (operation as? OperationState.Running)?.takeIf { it.id == operationId }
            val busy = ownOperation != null
            val anyBusy = operation is OperationState.Running
            val result = history.firstOrNull { it.id == operationId }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
                if (tree != null && !anyBusy) {
                    runCatching { model.receiveDocuments(uris, tree) }.fold(
                        onSuccess = { operationId = it; launchFailed = it == null },
                        onFailure = { launchFailed = true })
                }
            }
            VoyagerTheme(appTheme = theme, appearance = appearance) {
                BackHandler(enabled = started && busy) { operationId?.let(model::cancelReceivedOperation) }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.finderdex_receive_title), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(when {
                            uris.isEmpty() -> R.string.finderdex_receive_invalid
                            launchFailed -> R.string.finderdex_receive_failed
                            !started -> R.string.finderdex_receive_ready
                            busy -> R.string.progress_uploading
                            result?.outcome == OperationOutcome.COMPLETED -> R.string.finderdex_receive_done
                            else -> R.string.finderdex_receive_failed
                        }))
                        if (started && busy) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            ownOperation?.progress?.let {
                                Text("${it.completedItems} / ${it.totalItems ?: "—"}")
                            }
                        }
                        if (!started) Button(enabled = uris.isNotEmpty() && !anyBusy, onClick = { picker.launch(null) }) {
                            Text(stringResource(R.string.finderdex_receive_choose))
                        }
                        TextButton(onClick = { if (started && busy) operationId?.let(model::cancelReceivedOperation) else finish() }) {
                            Text(stringResource(if (busy && started) R.string.action_cancel else R.string.finderdex_close))
                        }
                    }
                }
                if (busy) conflict?.let { request ->
                    TransferConflictDialog(request) { model.resolveTransferConflict(request, it) }
                }
            }
        }
    }

}

internal object SharedFileIntents {
    fun incoming(source: Intent): List<Uri> = runCatching { parse(source) }.getOrDefault(emptyList())

    @Suppress("DEPRECATION")
    private fun parse(source: Intent): List<Uri> {
        if (source.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return emptyList()
        val streams = if (source.action == Intent.ACTION_SEND_MULTIPLE) {
            if (Build.VERSION.SDK_INT >= 33) source.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else source.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        } else {
            listOfNotNull(if (Build.VERSION.SDK_INT >= 33) source.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        }
        val uris = streams.orEmpty().ifEmpty {
            source.clipData?.let { clip ->
                if (clip.itemCount > 64) return emptyList()
                (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
            }.orEmpty()
        }
        if (uris.size !in 1..64) return emptyList()
        return uris.distinct().takeIf { values -> values.all {
            it.scheme == "content" && !it.authority.isNullOrBlank()
        } }.orEmpty()
    }
}
