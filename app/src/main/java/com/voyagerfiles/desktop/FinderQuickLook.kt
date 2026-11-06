package com.voyagerfiles.desktop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import com.voyagerfiles.ui.theme.LocalFinderMotion
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voyagerfiles.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.DateFormat

private sealed interface QuickLookContent {
    data object Loading : QuickLookContent
    data class Picture(val bitmap: Bitmap) : QuickLookContent
    data class Document(val text: String) : QuickLookContent
    data class Unavailable(val reason: String) : QuickLookContent
}

/** Bounded local-only decoder. No network requests and no writes to user files. */
private fun decodeQuickLook(file: FileItem): QuickLookContent {
    if (file.isDirectory) return QuickLookContent.Unavailable("Carpeta")
    if (file.source != com.voyagerfiles.data.model.FileSource.LOCAL)
        return QuickLookContent.Unavailable("Vista previa del proveedor no disponible")
    val path = File(file.path)
    if (!path.isFile || !path.canRead()) return QuickLookContent.Unavailable("No hay permiso de lectura")
    return try {
        when {
            file.isImage -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    QuickLookContent.Unavailable("Imagen no compatible")
                } else {
                    val sample = generateSequence(1) { it * 2 }.first {
                        bounds.outWidth / it <= 1800 && bounds.outHeight / it <= 1800
                    }
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val image = BitmapFactory.decodeFile(file.path, options)
                    if (image == null) QuickLookContent.Unavailable("No se pudo decodificar la imagen")
                    else QuickLookContent.Picture(image)
                }
            }
            file.isPdf -> {
                ParcelFileDescriptor.open(path, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount == 0) return QuickLookContent.Unavailable("PDF vacío")
                        renderer.openPage(0).use { page ->
                            val scale = minOf(1800f / page.width, 1800f / page.height, 2f)
                            val bitmap = Bitmap.createBitmap(
                                maxOf(1, (page.width * scale).toInt()),
                                maxOf(1, (page.height * scale).toInt()),
                                Bitmap.Config.ARGB_8888,
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            QuickLookContent.Picture(bitmap)
                        }
                    }
                }
            }
            file.isText -> {
                val bytes = ByteArray(128 * 1024)
                val read = FileInputStream(path).use { it.read(bytes) }
                val content = if (read > 0) String(bytes, 0, read, Charsets.UTF_8) else ""
                QuickLookContent.Document(content + if (path.length() > bytes.size) "\n\n… Vista previa limitada a 128 KB" else "")
            }
            else -> QuickLookContent.Unavailable("No hay vista previa para este formato")
        }
    } catch (e: Exception) {
        QuickLookContent.Unavailable(e.localizedMessage ?: "No se pudo leer el archivo")
    } catch (e: OutOfMemoryError) {
        QuickLookContent.Unavailable("El archivo supera la memoria disponible")
    }
}

@Composable
internal fun FinderQuickLookDialog(file: FileItem, onDismiss: () -> Unit,
    onNavigate: (Int) -> Unit = {}) {
    val focus = remember { FocusRequester() }
    val motion = LocalFinderMotion.current
    val colors = MaterialTheme.colorScheme
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    var closing by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    val close = { visibility.targetState = false; closing = true }
    // Wait for the actual transition, including Android's animation duration scale.
    LaunchedEffect(visibility.isIdle, visibility.currentState, closing) {
        if (closing && visibility.isIdle && !visibility.currentState) dismiss()
    }
    var content by remember(file.path) { mutableStateOf<QuickLookContent>(QuickLookContent.Loading) }
    LaunchedEffect(file.path) {
        val decoded = withContext(Dispatchers.IO) { decodeQuickLook(file) }
        content = decoded
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(visibleState = visibility,
            enter = fadeIn(tween(motion.duration(160))) + scaleIn(tween(motion.duration(160)), initialScale = if (motion.spatial) .96f else 1f),
            exit = fadeOut(tween(motion.duration(120))) + scaleOut(tween(motion.duration(120)), targetScale = if (motion.spatial) .98f else 1f)) {
        Column(
            Modifier.fillMaxWidth(.74f).fillMaxHeight(.82f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(colors.surface)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else when (event.key) {
                        Key.Escape, Key.Spacebar -> { close(); true }
                        Key.DirectionRight, Key.DirectionDown -> { onNavigate(1); true }
                        Key.DirectionLeft, Key.DirectionUp -> { onNavigate(-1); true }
                        else -> false
                    }
                }
                .focusable()
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(file.name, color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                    maxLines = 1, modifier = Modifier.weight(1f))
                Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_quicklook_close), color = colors.primary, modifier = Modifier.padding(start = 12.dp).clickable(onClick = close), fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (val preview = content) {
                    is QuickLookContent.Loading -> Text(androidx.compose.ui.res.stringResource(com.voyagerfiles.R.string.finderdex_quicklook_loading))
                    is QuickLookContent.Picture -> Image(preview.bitmap.asImageBitmap(), null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    is QuickLookContent.Document -> Box(Modifier.fillMaxSize()
                        .background(colors.surfaceContainerHigh, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Text(preview.text, color = colors.onSurface, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                    is QuickLookContent.Unavailable -> Text(preview.reason, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${file.formattedSize}   ·   ${file.mimeType}   ·   ${DateFormat.getDateTimeInstance().format(file.lastModified)}",
                color = colors.onSurfaceVariant, fontSize = 11.sp)
        }
        LaunchedEffect(file.path) { focus.requestFocus() }
        }
    }
}
