package com.voyagerfiles.desktop

import android.os.StatFs
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.voyagerfiles.R
import com.voyagerfiles.data.model.FileItem
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class FinderViewDensity(
    val iconDp: Float, val cellHeightDp: Float, val gridMinimumDp: Float,
    val rowHeightDp: Float, val stripHeightDp: Float
)
internal object FinderZoomPresets {
    private val icons = listOf(64f,84f,104f,128f,160f)
    private val details = listOf(24f,30f,37f,44f)
    private val columns = listOf(25f,30f,35f,42f)
    private val gallery = listOf(58f,70f,76f,94f,112f)
    fun maxIndex(view: FinderViewStyle) =
        if (view == FinderViewStyle.ICONS || view == FinderViewStyle.GALLERY) 4 else 3
    fun clamp(view: FinderViewStyle, index: Int) = index.coerceIn(0,maxIndex(view))
    fun labels(view: FinderViewStyle): List<String> =
        if (maxIndex(view)==4) listOf("Muy chico","Chico","Mediano","Grande","Gigante")
        else listOf("Muy chico","Chico","Mediano","Grande")
    fun density(view: FinderViewStyle, raw: Int): FinderViewDensity {
        val i = clamp(view,raw)
        return when(view) {
            FinderViewStyle.ICONS -> FinderViewDensity(icons[i],icons[i]+56f,icons[i]+45f,37f,150f)
            FinderViewStyle.LIST -> FinderViewDensity((details[i]-11f).coerceAtLeast(15f),156f,146f,details[i],150f)
            FinderViewStyle.COLUMNS -> FinderViewDensity((columns[i]-11f).coerceAtLeast(16f),156f,146f,columns[i],150f)
            FinderViewStyle.GALLERY -> FinderViewDensity(gallery[i],156f,146f,37f,gallery[i]+74f)
        }
    }
}

@Composable
internal fun FinderPathBar(controller: FinderColumnController, columns: List<FinderColumn>,
    sidebarExtentDp: Float, compact: Boolean) {
    val p = LocalFinderMaterials.current.palette
    val clipboard = LocalClipboardManager.current
    val scroll = rememberScrollState()
    var menu by remember { mutableStateOf(false) }
    val path = controller.currentPath
    LaunchedEffect(path) { scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.fillMaxWidth().background(p.footer)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.separator))
        Row(Modifier.fillMaxWidth().height(32.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.width(sidebarExtentDp.dp).fillMaxHeight().background(p.sidebar))
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxSize().horizontalScroll(scroll).padding(horizontal=12.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    columns.forEachIndexed { i,col ->
                        if (i>0) Text("›",color=p.secondaryInk,fontSize=13.sp,
                            modifier=Modifier.padding(horizontal=7.dp))
                        FinderPathSegment(col.path, i==columns.lastIndex, compact,
                            onClick={ controller.navigateColumn(i) }, onContext={menu=true})
                    }
                }
                Box(Modifier.align(Alignment.CenterEnd)) {
                    Text("⋯",color=p.ink,modifier=Modifier.clickable { menu=true }.padding(8.dp))
                    FinderGlassMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text(stringResource(R.string.finderdex_path_copy))},
                            onClick={clipboard.setText(AnnotatedString(path));menu=false})
                        DropdownMenuItem(text={Text(stringResource(R.string.finderdex_path_parent))},
                            enabled=columns.size>1,onClick={
                                if(columns.size>1)controller.navigateColumn(columns.lastIndex-1)
                                menu=false
                            })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FinderPathSegment(path:String,current:Boolean,compact:Boolean,
    onClick:()->Unit,onContext:()->Unit) {
    val p=LocalFinderMaterials.current.palette
    val tracker=LocalFinderControlFocus.current
    val interaction=remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var tooltip by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        tooltip=false
        if(hovered) { delay(580);tooltip=true }
    }
    Box {
        Text(path.substringAfterLast('/').ifEmpty{"/"},
            color=if(current)p.ink else p.secondaryInk,fontSize=11.sp,
            fontWeight=if(current)FontWeight.SemiBold else FontWeight.Normal,
            maxLines=1,overflow=TextOverflow.Ellipsis,
            modifier=Modifier.widthIn(max=if(compact)126.dp else 190.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if(hovered)p.hover else androidx.compose.ui.graphics.Color.Transparent)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .finderRightClick(onContext)
                .onFocusChanged{tracker.update("breadcrumb:"+path,it.isFocused)}
                .combinedClickable(onClick=onClick,onLongClick=onContext)
                .semantics{contentDescription=path}
                .padding(horizontal=4.dp,vertical=5.dp))
        if(tooltip)Popup(alignment=Alignment.BottomCenter,
            properties=PopupProperties(focusable=false)) {
            Text(path,color=p.ink,fontSize=11.sp,
                modifier=Modifier.clip(RoundedCornerShape(7.dp))
                    .background(p.titlebar).border(.8.dp,p.edge,RoundedCornerShape(7.dp))
                    .padding(7.dp))
        }
    }
}

@Composable
internal fun FinderStatusBar(controller: FinderColumnController, columns: List<FinderColumn>,
    sidebarExtentDp: Float, heightDp: Float, compact: Boolean,
    viewStyle: FinderViewStyle, zoomIndex: Int, onZoom: (Int)->Unit) {
    val p=LocalFinderMaterials.current.palette
    val path=controller.currentPath
    var free by remember(path) { mutableStateOf<Long?>(null) }
    LaunchedEffect(path) {
        free=withContext(Dispatchers.IO) {runCatching{StatFs(path).availableBytes}.getOrNull()}
    }
    val current=columns.lastOrNull()
    val selected=current?.files.orEmpty().filter{it.path in current?.selected.orEmpty()}
    val bytes=selected.filterNot{it.isDirectory}.sumOf{it.size.coerceAtLeast(0L)}
    Row(Modifier.fillMaxWidth().height(heightDp.dp).background(p.footer),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(sidebarExtentDp.dp).fillMaxHeight().background(p.sidebar))
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val tight=maxWidth<430.dp
            Row(Modifier.fillMaxSize().padding(horizontal=12.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                val count=current?.files?.size?:0
                val label=if(tight)count.toString()+" · "+selected.size
                    else count.toString()+" elementos · "+selected.size+" seleccionados"+
                        if(selected.isNotEmpty())" · "+FileItem.formatFileSize(bytes) else ""
                Text(label,color=p.secondaryInk,fontSize=11.sp,maxLines=1,
                    overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                if(!tight)free?.let{
                    Text(FileItem.formatFileSize(it)+" libres",color=p.secondaryInk,fontSize=11.sp,maxLines=1)
                }
                FinderZoomControl(viewStyle,zoomIndex,onZoom,tight)
            }
        }
    }
}

@Composable
private fun FinderZoomControl(view:FinderViewStyle,selected:Int,onChange:(Int)->Unit,compact:Boolean) {
    val p=LocalFinderMaterials.current.palette
    val max=FinderZoomPresets.maxIndex(view)
    var menu by remember { mutableStateOf(false) }
    val tracker=LocalFinderControlFocus.current
    if(compact) Box {
        Text(stringResource(R.string.finderdex_zoom_decrease)+" / "+
                stringResource(R.string.finderdex_zoom_increase),color=p.ink,fontSize=11.sp,
            modifier=Modifier.clip(RoundedCornerShape(8.dp)).clickable{menu=true}
                .onFocusChanged{tracker.update("zoom:compact",it.isFocused)}
                .semantics{contentDescription="Ajustar tamaño de iconos"}.padding(7.dp))
        FinderGlassMenu(expanded=menu,onDismissRequest={menu=false}){
            val labels = listOf(
                stringResource(R.string.finderdex_zoom_very_small),
                stringResource(R.string.finderdex_zoom_small),
                stringResource(R.string.finderdex_zoom_medium),
                stringResource(R.string.finderdex_zoom_large),
                stringResource(R.string.finderdex_zoom_giant))
                .take(max + 1)
            labels.forEachIndexed {i,label->
                DropdownMenuItem(text={Text(label)},
                    leadingIcon={if(i==selected)Text("✓")},
                    onClick={onChange(i);menu=false})
            }
        }
    }else{
        Text(stringResource(R.string.finderdex_zoom_decrease),color=p.secondaryInk,fontSize=10.sp)
        Slider(value=selected.toFloat(),onValueChange={onChange(it.toInt().coerceIn(0,max))},
            valueRange=0f..max.toFloat(),steps=max-1,
            modifier=Modifier.width(116.dp).height(32.dp)
                .onFocusChanged{tracker.update("zoom:slider",it.isFocused)}
                .semantics{contentDescription="Tamaño de iconos"},
            colors=SliderDefaults.colors(thumbColor=p.accent,activeTrackColor=p.accent,
                inactiveTrackColor=p.separator))
        Text(stringResource(R.string.finderdex_zoom_increase),color=p.secondaryInk,fontSize=11.sp)
    }
}
