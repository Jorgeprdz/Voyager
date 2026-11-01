package com.voyagerfiles.desktop

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.view.DragEvent
import android.view.View
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.util.FileUtils
import java.io.File
import java.net.URI

/**
 * Global Android drags use ephemeral content:// grants, unlike FinderDex's internal pointer drags.
 * Returning true from startDragAndDrop does NOT mean the receiving app saved anything.
 * Source files are never removed by this feature.
 */
internal object FinderCrossAppTransfer {
    fun start(view: View, files: List<FileItem>): Boolean {
        if (files.isEmpty() || files.any { it.isDirectory }) return false
        val intent = FileUtils.createShareIntent(view.context, files).getOrElse { return false }
        val clip = intent.clipData ?: return false
        return view.startDragAndDrop(
            clip,
            View.DragShadowBuilder(view),
            null,
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ,
        )
    }

    fun activity(context: Context): Activity? {
        var current = context
        repeat(10) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    fun incomingUris(event: DragEvent): List<android.net.Uri> {
        val clip = event.clipData ?: return emptyList()
        if (clip.itemCount !in 1..64) return emptyList()
        val uris = (0 until clip.itemCount).map { clip.getItemAt(it).uri ?: return emptyList() }
        return if (FinderCrossAppPolicy.accepts(uris.map { it.toString() }, true, false)) uris.distinct() else emptyList()
    }

    fun writableLocalFolder(path: String): Boolean = File(path).let { it.isDirectory && it.canWrite() }
}

internal object FinderCrossAppPolicy {
    /** Accept only properly formed content URIs with an authority. Never consume file:// drags. */
    fun accepts(uris: List<String>, destinationWritable: Boolean, operationBusy: Boolean): Boolean {
        if (!destinationWritable || operationBusy || uris.isEmpty() || uris.size > 64) return false
        return uris.all { value ->
            runCatching {
                val uri = URI(value)
                uri.scheme.equals("content", ignoreCase = true) &&
                    !uri.rawAuthority.isNullOrBlank() && uri.rawFragment == null
            }.getOrDefault(false)
        }
    }

    /** External Android drag is copy-only; successful drop never authorizes deleting originals. */
    val deletesOriginal: Boolean get() = false
}
