package com.voyagerfiles.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PathBreadcrumb(
    path: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    rootPath: String = "/",
) {
    val scrollState = rememberScrollState()
    val root = rootPath.trimEnd('/').ifEmpty { "/" }
    val relative = if (root != "/" && (path == root || path.startsWith("$root/"))) path.removePrefix(root) else path
    val base = if (relative != path || path == root) root else "/"
    val parts = relative.split("/").filter { it.isNotEmpty() }

    LaunchedEffect(path) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (base == "/") "/" else base.substringAfterLast('/'),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigate(base) }
                .padding(horizontal = 10.dp, vertical = 14.dp),
        )

        parts.forEachIndexed { index, part ->
            Icon(
                Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val targetPath = base.trimEnd('/') + "/" + parts.subList(0, index + 1).joinToString("/")
            val isLast = index == parts.lastIndex
            Text(
                text = part,
                style = MaterialTheme.typography.labelLarge,
                color = if (isLast) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onNavigate(targetPath) }
                    .padding(horizontal = 10.dp, vertical = 14.dp),
            )
        }
    }
}
