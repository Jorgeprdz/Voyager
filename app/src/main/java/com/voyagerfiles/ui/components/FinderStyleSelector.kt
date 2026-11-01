package com.voyagerfiles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.voyagerfiles.R
import com.voyagerfiles.ui.theme.*

/** Uses the same semantic tokens as the real app. Preview components are inert. */
@Composable
fun FinderStyleSelector(value: FinderAppearance, onChange: (FinderAppearance) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.finderdex_appearance_title), style = MaterialTheme.typography.titleMedium)
        FinderStyle.entries.forEach { style ->
            val sample = FinderAppearanceResolver.tokens(value.copy(style = style),
                FinderAppearanceResolver.resolveDark(value.brightness,
                    MaterialTheme.colorScheme.background.red < .25f))
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
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(15.dp))
                    .clickable { onChange(value.copy(style = style)) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    Modifier.size(84.dp, 56.dp)
                        .background(sample.background, RoundedCornerShape(10.dp)).padding(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(9.dp).background(
                        sample.navigation, RoundedCornerShape(3.dp)))
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.width(18.dp).fillMaxHeight().background(
                            sample.raised, RoundedCornerShape(3.dp)))
                        Column(Modifier.weight(1f).fillMaxHeight()
                            .background(sample.surface, RoundedCornerShape(4.dp)).padding(4.dp)) {
                            Box(Modifier.fillMaxWidth().height(6.dp).background(
                                sample.accent, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.height(3.dp))
                            Box(Modifier.fillMaxWidth(.75f).height(5.dp).background(
                                sample.border, RoundedCornerShape(2.dp)))
                        }
                    }
                }
                Text(stringResource(title), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium)
                RadioButton(selected = active, onClick = { onChange(value.copy(style = style)) })
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
