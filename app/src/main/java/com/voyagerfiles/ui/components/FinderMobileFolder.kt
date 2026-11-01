package com.voyagerfiles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voyagerfiles.ui.theme.FinderStyle
import com.voyagerfiles.ui.theme.LocalFinderSemanticTokens

/** The folder silhouette stays the same across themes; its material is style-specific. */
@Composable
internal fun FinderMobileFolder(modifier: Modifier = Modifier) {
    val theme = LocalFinderSemanticTokens.current
    Canvas(modifier.size(36.dp)) {
        val w = size.width
        val h = size.height
        if (theme.appearance.style == FinderStyle.GLASS) {
            // Exact existing mobile artwork on default Glass, with its gentle volume.
            drawRoundRect(Color(0x15203B63), Offset(w*.04f,h*.32f), Size(w*.94f,h*.60f), CornerRadius(w*.09f))
            drawRoundRect(Color(0xFF2685D8), Offset(w*.04f,h*.18f), Size(w*.43f,h*.28f), CornerRadius(w*.06f))
            drawRoundRect(Color(0xFF378CDD), Offset(w*.04f,h*.28f), Size(w*.92f,h*.56f), CornerRadius(w*.07f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF91DBFF), Color(0xFF48A5EE))),
                Offset(0f,h*.38f), Size(w,h*.49f), CornerRadius(w*.07f))
            drawLine(Color.White.copy(alpha=.65f), Offset(w*.08f,h*.40f),Offset(w*.90f,h*.40f), w*.02f)
        } else {
            val material = theme.appearance.style == FinderStyle.MATERIAL
            val fluent = theme.appearance.style == FinderStyle.FLUENT
            val radius = if (fluent) w*.035f else if (material) w*.11f else w*.09f
            drawRoundRect(theme.folder, Offset(w*.05f,h*.18f),
                Size(w*.46f,h*.31f), CornerRadius(radius))
            drawRoundRect(theme.folder, Offset(w*.035f,h*.29f),
                Size(w*.93f,h*.59f), CornerRadius(radius))
            drawRoundRect(
                if (material) theme.folder else theme.folderFront,
                Offset(w*.035f,h*.39f),
                Size(w*.93f,h*.49f), CornerRadius(radius))
            if (fluent) {
                drawLine(theme.folder, Offset(w*.07f,h*.42f),Offset(w*.91f,h*.42f), w*.017f)
            } else if (!material) {
                drawLine(theme.folderFront.copy(alpha = .56f),
                    Offset(w*.10f,h*.41f),Offset(w*.91f,h*.41f), w*.014f)
            }
        }
    }
}
