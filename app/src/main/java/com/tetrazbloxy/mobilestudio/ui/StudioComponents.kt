package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.net.ImageLoader
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun StudioPanel(
    modifier: Modifier = Modifier,
    color: Color = LocalStudioPalette.current.panel,
    content: @Composable () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        content()
    }
}

@Composable
fun PanelHeader(
    title: String,
    glyph: Glyph? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(palette.header)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            StudioIcon(glyph, palette.accent, size = 17.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = palette.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

@Composable
fun StudioIconButton(
    glyph: Glyph,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalStudioPalette.current.text,
    background: Color = Color.Transparent,
    size: Dp = 36.dp,
    iconSize: Dp = 19.dp,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val palette = LocalStudioPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) palette.hover else background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center
    ) {
        StudioIcon(glyph, tint, size = iconSize)
    }
}

@Composable
fun StudioButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: Glyph? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val palette = LocalStudioPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = when {
        !primary && pressed -> palette.hover
        primary -> palette.accent
        else -> palette.card
    }
    val textColor = if (primary) Color.White else palette.text
    Row(
        modifier = modifier
            .heightIn(min = if (compact) 34.dp else 42.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(
                1.dp,
                if (primary) Color.Transparent else palette.border,
                RoundedCornerShape(11.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = if (compact) 12.dp else 16.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (glyph != null) {
            StudioIcon(glyph, textColor, size = 17.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun RibbonButton(
    label: String,
    glyph: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true
) {
    val palette = LocalStudioPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .width(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) palette.hover else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 3.dp)
            .alpha(if (enabled) 1f else 0.4f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StudioIcon(glyph, tint ?: palette.accent, size = 23.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = palette.text,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun StudioField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    textStyle: TextStyle? = null
) {
    val palette = LocalStudioPalette.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .alpha(if (enabled) 1f else 0.55f),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle ?: TextStyle(
            color = palette.text,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif
        ),
        cursorBrush = SolidColor(palette.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        color = palette.dim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(Glyph.SEARCH, palette.dim, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.text, fontSize = 13.sp),
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = palette.dim, fontSize = 13.sp)
                    }
                    inner()
                }
            )
        }
        if (value.isNotEmpty()) {
            StudioIconButton(
                glyph = Glyph.CLOSE,
                contentDescription = "Clear search",
                onClick = { onValueChange("") },
                tint = palette.dim,
                size = 26.dp,
                iconSize = 14.dp
            )
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(description, color = palette.dim, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.dim,
                uncheckedTrackColor = palette.input,
                uncheckedBorderColor = palette.border
            )
        )
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueLabel: (Float) -> String = { "%.2f".format(it) }
) {
    val palette = LocalStudioPalette.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(valueLabel(value), color = palette.accent, fontSize = 12.sp)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.border
            )
        )
    }
}

@Composable
fun SelectRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = palette.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = palette.dim,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(4.dp))
        StudioIcon(Glyph.CHEVRON_RIGHT, palette.dim, size = 15.dp)
    }
}

@Composable
fun StudioDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalStudioPalette.current.border)
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = LocalStudioPalette.current.dim,
        fontSize = 10.sp,
        letterSpacing = 1.1.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun TagChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(
    glyph: Glyph,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = modifier.fillMaxWidth().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.card),
            contentAlignment = Alignment.Center
        ) {
            StudioIcon(glyph, palette.accent, size = 30.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            color = palette.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = palette.dim,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun StudioDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selected: String? = null
) {
    val palette = LocalStudioPalette.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .widthIn(min = 180.dp)
            .background(palette.card)
    ) {
        for (item in items) {
            DropdownMenuItem(
                text = {
                    Text(
                        item,
                        color = if (item == selected) palette.accent else palette.text,
                        fontSize = 13.sp
                    )
                },
                onClick = {
                    onSelect(item)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun ToastHost(session: StudioSession) {
    val toast = session.toast ?: return
    val palette = LocalStudioPalette.current
    LaunchedEffect(toast.id) {
        delay(2_800)
        session.dismissToast(toast.id)
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = palette.card,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioIcon(Glyph.INFO, palette.accent, size = 17.dp)
                Spacer(Modifier.width(9.dp))
                Text(toast.text, color = palette.text, fontSize = 12.sp, maxLines = 3)
            }
        }
    }
}

@Composable
fun RemoteImage(
    loader: ImageLoader,
    source: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackGlyph: Glyph = Glyph.IMAGE
) {
    val palette = LocalStudioPalette.current
    var bitmap by remember(source) { mutableStateOf(loader.cached(source)) }
    LaunchedEffect(source) {
        if (bitmap == null && !source.isNullOrBlank()) {
            bitmap = withContext(Dispatchers.IO) { loader.load(source) }
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(palette.card),
            contentAlignment = Alignment.Center
        ) {
            StudioIcon(fallbackGlyph, palette.dim, size = 26.dp)
        }
    }
}

@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (option in options) {
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) palette.accent else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    color = if (active) Color.White else palette.dim,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

fun formatRelativeTime(time: Long): String {
    if (time <= 0L) return "never"
    val delta = (System.currentTimeMillis() - time).coerceAtLeast(0L)
    val minutes = delta / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} d ago"
        else -> {
            val days = minutes / (60 * 24)
            "${days / 7} w ago"
        }
    }
}

fun formatClock(time: Long): String = try {
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(time))
} catch (_: Throwable) {
    ""
}
