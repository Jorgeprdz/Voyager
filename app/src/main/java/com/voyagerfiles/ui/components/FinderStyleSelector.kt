package com.voyagerfiles.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.ui.theme.*

/** Four live styles. Preview contains toolbar, sidebar, folder and selected file. */
@Composable
fun FinderStyleSelector(value: FinderAppearance, onChange: (FinderAppearance) -> Unit) {
    val systemDark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.finderdex_appearance_title),
            style = MaterialTheme.typography.titleMedium)
        FinderStyle.entries.forEach { style ->
            val sample = FinderAppearanceResolver.tokens(value.copy(style = style),
                FinderAppearanceResolver.resolveDark(value.brightness, systemDark))
            val title = when (style) {
                FinderStyle.GLASS -> R.string.finderdex_style_glass
                FinderStyle.MATERIAL -> R.string.finderdex_style_material
                FinderStyle.ONE_UI -> R.string.finderdex_style_oneui
                FinderStyle.FLUENT -> R.string.finderdex_style_fluent
            }
            val active = value.style == style
            Row(
                Modifier.fillMaxWidth().semantics { selected = active }
                    .border(if (active) 2.dp else 1.dp,
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(15.dp))
                    .clickable { onChange(value.copy(style = style)) }
                    .padding(10.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    Modifier.size(116.dp, 74.dp)
                        .background(sample.background, RoundedCornerShape(10.dp))
                        .padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // App-owned toolbar. Not an OS window control or an interactive mockup.
                    Row(Modifier.fillMaxWidth().height(10.dp)
                        .background(sample.raised, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(Modifier.size(3.dp).background(sample.secondaryText,
                                RoundedCornerShape(2.dp)))
                        }
                    }
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.width(24.dp).fillMaxHeight()
                            .background(sample.navigation, RoundedCornerShape(3.dp))
                            .padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.fillMaxWidth().height(5.dp)
                                .background(sample.accent, RoundedCornerShape(2.dp)))
                            Box(Modifier.fillMaxWidth().height(3.dp)
                                .background(sample.secondaryText, RoundedCornerShape(2.dp)))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()
                            .background(sample.surface, RoundedCornerShape(4.dp))
                            .padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(13.dp, 10.dp)
                                    .background(sample.folder, RoundedCornerShape(2.dp)))
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.weight(1f).height(3.dp)
                                    .background(sample.secondaryText, RoundedCornerShape(2.dp)))
                            }
                            Row(Modifier.fillMaxWidth()
                                .background(sample.selected, RoundedCornerShape(sample.selectionRadiusDp.dp))
                                .padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(5.dp).background(sample.accent,
                                    RoundedCornerShape(1.dp)))
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.fillMaxWidth(.72f).height(3.dp)
                                    .background(sample.onSelected, RoundedCornerShape(2.dp)))
                            }
                        }
                    }
                }
                Text(stringResource(title), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium)
                RadioButton(selected = active,
                    onClick = { onChange(value.copy(style = style)) })
            }
        }
        Text(stringResource(R.string.finderdex_motion_title), style = MaterialTheme.typography.titleSmall)
        com.voyagerfiles.ui.theme.FinderMotionMode.entries.forEach { mode ->
            val title = when (mode) {
                com.voyagerfiles.ui.theme.FinderMotionMode.FULL -> R.string.finderdex_motion_full
                com.voyagerfiles.ui.theme.FinderMotionMode.REDUCED -> R.string.finderdex_motion_reduced
                com.voyagerfiles.ui.theme.FinderMotionMode.OFF -> R.string.finderdex_motion_off
            }
            Row(Modifier.fillMaxWidth().clickable { onChange(value.copy(motion = mode)) }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = value.motion == mode, onClick = null)
                Text(stringResource(title))
            }
        }
        Text(stringResource(R.string.finderdex_brightness_title),
            style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FinderBrightness.entries.forEach { mode ->
                val name = when (mode) {
                    FinderBrightness.AUTOMATIC -> R.string.finderdex_brightness_auto
                    FinderBrightness.LIGHT -> R.string.finderdex_brightness_light
                    FinderBrightness.DARK -> R.string.finderdex_brightness_dark
                }
                FilterChip(selected = value.brightness == mode,
                    onClick = { onChange(value.copy(brightness = mode)) },
                    label = { Text(stringResource(name)) })
            }
        }
        if (value.style == FinderStyle.MATERIAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.finderdex_dynamic_colors), Modifier.weight(1f))
                Switch(checked = value.dynamicColors,
                    onCheckedChange = { onChange(value.copy(dynamicColors = it)) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.finderdex_high_contrast), Modifier.weight(1f))
            Switch(checked = value.highContrast,
                onCheckedChange = { onChange(value.copy(highContrast = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.finderdex_reduce_transparency), Modifier.weight(1f))
            Switch(checked = value.reduceTransparency,
                onCheckedChange = { onChange(value.copy(reduceTransparency = it)) })
        }
    }
}
