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

@Composable
internal fun FinderMobileFolder(modifier: Modifier = Modifier) {
    Canvas(modifier.size(36.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0x15203B63), Offset(w*.04f,h*.32f), Size(w*.94f,h*.60f), CornerRadius(w*.09f))
        drawRoundRect(Color(0xFF2685D8), Offset(w*.04f,h*.18f), Size(w*.43f,h*.28f), CornerRadius(w*.06f))
        drawRoundRect(Color(0xFF378CDD), Offset(w*.04f,h*.28f), Size(w*.92f,h*.56f), CornerRadius(w*.07f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF91DBFF), Color(0xFF48A5EE))),
            Offset(0f,h*.38f), Size(w,h*.49f), CornerRadius(w*.07f))
        drawLine(Color.White.copy(alpha=.65f), Offset(w*.08f,h*.40f),Offset(w*.90f,h*.40f), w*.02f)
    }
}
