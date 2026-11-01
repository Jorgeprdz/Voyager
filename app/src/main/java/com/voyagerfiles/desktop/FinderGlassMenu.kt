package com.voyagerfiles.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.voyagerfiles.ui.theme.LocalFinderSemanticTokens

/**
 * App-owned popup only. Keeps Material3 keyboard/ESC/outside-click positioning while
 * overriding the actual popup surface color, not merely the menu item backgrounds.
 */
@Composable
internal fun FinderGlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p=LocalFinderMaterials.current.palette
    val t=LocalFinderSemanticTokens.current
    val shape=RoundedCornerShape(t.menuRadiusDp.dp)
    val colors=MaterialTheme.colorScheme.copy(
        surface=p.titlebar, surfaceContainer=p.titlebar,
        onSurface=p.ink, onSurfaceVariant=p.secondaryInk)
    MaterialTheme(colorScheme=colors) {
        DropdownMenu(expanded=expanded,onDismissRequest=onDismissRequest,
            modifier=Modifier.clip(shape).background(p.titlebar)
                .border(t.borderDp.dp,p.edge,shape)) {
            content()
        }
    }
}
