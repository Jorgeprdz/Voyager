package com.voyagerfiles.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.ui.components.FileThumbnailOrIcon
import java.text.DateFormat

private val ink = Color(0xff303034)
private val soft = Color(0xfff5f7fb)
private val blue = Color(0xffdfebfc)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FinderListView(
    controller: FinderColumnController,
    columns: List<FinderColumn>,
    query: String,
    descending: Boolean,
    modifier: Modifier,
    onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean,
    onAction: (FinderFileAction, FileItem) -> Unit,
) {
    val last = columns.lastOrNull()
    val files = remember(last?.files, query, descending) {
        last?.files.orEmpty().filter { it.name.contains(query, ignoreCase = true) }
            .let { if (descending) it.reversed() else it }
    }
    Column(modifier.background(Color.White).testTag("finder-list")) {
        Row(Modifier.fillMaxWidth().height(31.dp).background(Color(0xfff5f5f7))
            .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Nombre", Modifier.weight(1f), color = ink, fontSize = 11.sp)
            Text("Tamaño", Modifier.width(100.dp), color = ink, fontSize = 11.sp)
            Text("Modificado", Modifier.width(128.dp), color = ink, fontSize = 11.sp)
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 5.dp)) {
            items(files, key = { it.path }) { file ->
                var menu by remember { mutableStateOf(false) }
                var ctrl by remember { mutableStateOf(false) }
                var shift by remember { mutableStateOf(false) }
                val hoverSource = remember { MutableInteractionSource() }
                val hovered by hoverSource.collectIsHoveredAsState()
                val selected = file.path in last?.selected.orEmpty()
                Row(Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 9.dp, vertical = 1.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) blue else if (hovered) soft else Color.Transparent)
                    .hoverable(hoverSource)
                    .finderMouseSelectionModifiers { c, sh -> ctrl = c; shift = sh }
                    .finderRightClick {
                        if (file.path !in last?.selected.orEmpty()) controller.selectOnly(columns.lastIndex, file)
                        menu = true
                    }
                    .combinedClickable(
                        onClick = { controller.selectOnly(columns.lastIndex, file, ctrl, shift) },
                        onDoubleClick = {
                            if (file.isDirectory) controller.select(columns.lastIndex, file) else onOpen(file)
                        },
                        onLongClick = { controller.selectOnly(columns.lastIndex, file); menu = true })
                    .semantics { this.selected = selected }
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(24.dp))
                    else FileThumbnailOrIcon(file, 24.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(file.name, Modifier.weight(1f), color = ink, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    Text(if (file.isDirectory) "—" else file.formattedSize,
                        Modifier.width(100.dp), color = ink, fontSize = 11.sp, maxLines = 1)
                    Text(DateFormat.getDateInstance(DateFormat.SHORT).format(file.lastModified),
                        Modifier.width(128.dp), color = ink, fontSize = 11.sp, maxLines = 1)
                    FinderFileMenu(menu, { menu = false }, file.isDirectory, operationsAvailable) {
                        onAction(it, file)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FinderGalleryView(
    controller: FinderColumnController,
    columns: List<FinderColumn>,
    query: String,
    descending: Boolean,
    modifier: Modifier,
    onOpen: (FileItem) -> Unit,
    operationsAvailable: Boolean,
    onAction: (FinderFileAction, FileItem) -> Unit,
) {
    val last = columns.lastOrNull()
    val files = remember(last?.files, query, descending) {
        last?.files.orEmpty().filter { it.name.contains(query, ignoreCase = true) }
            .let { if (descending) it.reversed() else it }
    }
    val selected = files.firstOrNull { it.path in last?.selected.orEmpty() }
    Column(modifier.background(Color.White).testTag("finder-gallery")) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (selected == null) {
                Text("Selecciona un archivo", fontSize = 13.sp, color = ink)
            } else {
                if (selected.isDirectory) FinderGlyph("folder", modifier = Modifier.size(188.dp))
                else FileThumbnailOrIcon(selected, 220.dp)
                Spacer(Modifier.height(12.dp))
                Text(selected.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ink)
                Spacer(Modifier.height(5.dp))
                Text(selected.formattedSize, fontSize = 12.sp, color = Color.Gray)
                if (!selected.isDirectory) Text("Doble clic para abrir", fontSize = 11.sp, color = Color.Gray)
            }
        }
        LazyRow(Modifier.fillMaxWidth().height(144.dp).background(Color(0xfff5f5f7))
            .padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(files, key = { it.path }) { file ->
                var menu by remember { mutableStateOf(false) }
                val active = file.path in last?.selected.orEmpty()
                Column(Modifier.width(105.dp).fillMaxHeight().clip(RoundedCornerShape(9.dp))
                    .background(if (active) blue else Color.Transparent)
                    .finderRightClick {
                        if (file.path !in last?.selected.orEmpty()) controller.selectOnly(columns.lastIndex, file)
                        menu = true
                    }
                    .combinedClickable(
                        onClick = { controller.selectOnly(columns.lastIndex, file) },
                        onDoubleClick = {
                            if (file.isDirectory) controller.select(columns.lastIndex, file) else onOpen(file)
                        },
                        onLongClick = { menu = true })
                    .padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (file.isDirectory) FinderGlyph("folder", modifier = Modifier.size(72.dp))
                    else FileThumbnailOrIcon(file, 72.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(file.name, color = ink, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    FinderFileMenu(menu, { menu = false }, file.isDirectory, operationsAvailable) {
                        onAction(it, file)
                    }
                }
            }
        }
    }
}
