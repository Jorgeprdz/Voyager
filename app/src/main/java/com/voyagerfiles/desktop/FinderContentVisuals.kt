package com.voyagerfiles.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.FileItem

/** Pure visual tokens; F1 window geometry and F2 selection colors remain authoritative. */
internal object FinderContentTokens {
    const val ICON_GRID_MIN_DP = 146f
    const val ICON_CELL_HEIGHT_DP = 156f
    const val ICON_FOLDER_SIZE_DP = 104f
    const val ICON_THUMBNAIL_DP = 98f
    const val ICON_CELL_CORNER_DP = 12f
    const val ICON_LABEL_CORNER_DP = 7f
    const val ICON_GRID_OUTER_HORIZONTAL_DP = 20f
    const val ICON_GRID_OUTER_VERTICAL_DP = 18f
    const val ICON_GRID_COLUMN_GAP_DP = 12f
    const val ICON_GRID_ROW_GAP_DP = 12f
    const val LIST_ROW_HEIGHT_DP = 37f
    const val LIST_HEADER_HEIGHT_DP = 34f
    const val LIST_ROW_CORNER_DP = 9f
    const val COLUMN_WIDTH_DP = 244f
    const val COLUMN_ROW_HEIGHT_DP = 30f
    const val COLUMN_ROW_CORNER_DP = 8f
    const val GALLERY_STRIP_HEIGHT_DP = 150f
    const val GALLERY_THUMB_CELL_DP = 112f
    const val GALLERY_THUMB_DP = 76f
    const val PREVIEW_PADDING_DP = 18f
    const val PREVIEW_FRAME_WIDTH_DP = 170f
    const val PREVIEW_FRAME_HEIGHT_DP = 150f
    const val PREVIEW_THUMBNAIL_DP = 134f
    const val PREVIEW_FOLDER_DP = 108f

    fun listColumns(widthDp: Float): FinderListColumns = FinderListColumns(
        showSize = widthDp >= 450f,
        showDate = widthDp >= 645f,
    )

    fun galleryHeroSize(widthDp: Float, heightDp: Float): Float =
        minOf(260f, (widthDp - 64f).coerceAtLeast(84f), (heightDp - 108f).coerceAtLeast(84f))
            .coerceAtLeast(84f)

    fun fileFormat(extension: String): String? = when (extension.lowercase().trimStart('.')) {
        "pdf" -> "PDF"
        "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp" -> "IMG"
        "mp4", "mkv", "avi", "mov", "webm" -> "VIDEO"
        "mp3", "m4a", "flac", "wav", "ogg", "aac" -> "AUDIO"
        "doc", "docx", "odt", "rtf" -> "DOC"
        "xls", "xlsx", "ods", "csv" -> "TAB"
        "ppt", "pptx", "odp" -> "PPT"
        "zip", "7z", "rar", "tar", "gz" -> "ZIP"
        "txt", "md" -> "TXT"
        else -> null
    }
}

internal data class FinderListColumns(val showSize: Boolean, val showDate: Boolean)

/** Original folder artwork colors; no system launcher assets or proprietary Apple resources. */
internal data class FinderFolderColors(
    val base: Color, val top: Color, val bottom: Color, val highlight: Color,
) {
    companion object {
        val Light = FinderFolderColors(
            Color(0xff8ad5f5), Color(0xff76d0f7), Color(0xff2e9ed9), Color(0xffc7f0ff))
        val Dark = FinderFolderColors(
            Color(0xff417ca8), Color(0xff56aada), Color(0xff2771b0), Color(0xff9cdaff))
    }
}

/** Lightweight format cue; thumbnails and image decoding stay in FileThumbnailOrIcon. */
@Composable
internal fun FinderFileFormatBadge(file: FileItem) {
    if (file.isDirectory) return
    val format = FinderContentTokens.fileFormat(file.extension) ?: return
    val p = LocalFinderMaterials.current.palette
    Box(Modifier.clip(RoundedCornerShape(5.dp))
        .background(p.titlebar)
        .border(0.7.dp, p.edge, RoundedCornerShape(5.dp))
        .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center) {
        Text(format, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
            color = p.secondaryInk, maxLines = 1)
    }
}
