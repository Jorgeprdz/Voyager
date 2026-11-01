package com.voyagerfiles.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A semantic heading, not a selectable/drag source or member of the file model. */
@Composable
internal fun FinderGroupHeader(name: String, count: Int, modifier: Modifier = Modifier) {
    val palette = LocalFinderMaterials.current.palette
    Text(
        "$name · $count",
        modifier = modifier.fillMaxWidth().background(palette.footer)
            .semantics { heading() }.padding(horizontal = 12.dp, vertical = 7.dp),
        color = palette.secondaryInk,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
    )
}
