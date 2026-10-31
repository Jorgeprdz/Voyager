package com.voyagerfiles.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.repository.LocalFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Spring-loaded folder overlay: loads real directories without switching FinderColumnController's
 * currentPath. The drag selection and its original source therefore remain unchanged.
 */
@Composable
internal fun FinderSpringFolderPanel(drag: FinderDragDropState, modifier: Modifier = Modifier) {
    val folder = drag.springFolder ?: return
    if (drag.draggingPaths.isEmpty()) return
    val children by produceState<Result<List<FileItem>>?>(null, folder) {
        value = withContext(Dispatchers.IO) {
            LocalFileProvider().listFiles(folder).map { items ->
                items.filter { it.isDirectory && !it.isHidden }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        }
    }
    Column(modifier.width(300.dp).heightIn(max = 390.dp)
        .shadow(14.dp, RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xfff9fafd))
        .border(1.dp, Color(0xffdae3f1), RoundedCornerShape(16.dp))
        .padding(12.dp)) {
        Text("Carpeta abierta temporalmente", color = Color(0xff667386), fontSize = 10.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(folder.substringAfterLast('/'), modifier = Modifier.weight(1f), maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Cerrar", Modifier.clickable { drag.closeSpringFolder() }.padding(5.dp),
                color = Color(0xff087cf1), fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(Color(0xffeaf2fd))
            .finderFolderDropTarget("spring-current:$folder", folder, drag)
            .finderDragVisual(folder, true, drag)
            .padding(9.dp)) {
            Text("Soltar aquí para ${if (drag.copyRequested) "copiar" else "mover"}",
                color = Color(0xff285e9e), fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        when (val result = children) {
            null -> Text("Abriendo…", color = Color.Gray, fontSize = 12.sp)
            else -> result.fold(
                onSuccess = { directories ->
                    if (directories.isEmpty()) Text("No hay subcarpetas", color = Color.Gray, fontSize = 12.sp)
                    else LazyColumn(Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(directories, key = { it.path }) { item ->
                            Row(Modifier.fillMaxWidth().height(35.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .finderFolderDropTarget("spring-child:${item.path}", item.path, drag)
                                .finderDragVisual(item.path, true, drag)
                                .background(Color(0xfff0f3f8))
                                .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                FinderGlyph("folder", modifier = Modifier.size(23.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(item.name, fontSize = 12.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                },
                onFailure = { Text(it.localizedMessage ?: "No se puede leer esta carpeta",
                    color = Color(0xffa12e32), fontSize = 12.sp) },
            )
        }
    }
}
