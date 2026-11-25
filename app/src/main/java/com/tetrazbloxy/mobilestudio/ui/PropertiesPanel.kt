package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.model.BasePart
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.Enums
import com.tetrazbloxy.mobilestudio.model.Humanoid
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.model.Vector3
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession

@Composable
fun PropertiesPanel(
    session: StudioSession,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Column(modifier = modifier.fillMaxSize().background(palette.panel)) {
        PanelHeader("Properties", Glyph.SETTINGS) {
            StudioIconButton(
                glyph = Glyph.CLOSE,
                contentDescription = "Close Properties",
                onClick = { session.toggleDock("properties") },
                tint = palette.dim,
                size = 30.dp,
                iconSize = 15.dp
            )
        }

        when (session.selection.size) {
            0 -> EmptyState(
                glyph = Glyph.SETTINGS,
                title = "Nothing selected",
                subtitle = "Tap a part in the viewport or pick an instance in the Explorer."
            )
            1 -> SingleProperties(session, controller, session.selection.first())
            else -> MultiProperties(session, controller)
        }
    }
}

@Composable
private fun MultiProperties(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(Glyph.LAYERS, palette.accent, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "${session.selection.size} instances selected",
                    color = palette.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Apply actions to the whole selection.", color = palette.dim, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        for (instance in session.selection.take(8)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioIcon(classGlyph(instance.className), classAccent(instance.className), size = 14.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    instance.name,
                    color = palette.text,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(instance.className, color = palette.dim, fontSize = 9.sp)
            }
        }
        if (session.selection.size > 8) {
            Text(
                "and ${session.selection.size - 8} more…",
                color = palette.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 21.dp, top = 2.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        StudioButton("Group into Model", { controller.groupSelection() }, glyph = Glyph.LAYERS)
        Spacer(Modifier.height(8.dp))
        StudioButton("Duplicate", { controller.duplicateSelection() }, glyph = Glyph.COPY)
        Spacer(Modifier.height(8.dp))
        StudioButton("Delete", { controller.deleteSelection() }, glyph = Glyph.TRASH)
        Spacer(Modifier.height(8.dp))
        StudioButton("Export selection", { controller.exportSelection() }, glyph = Glyph.UPLOAD)
    }
}

@Composable
private fun SingleProperties(
    session: StudioSession,
    controller: StudioController,
    instance: Instance
) {
    val palette = LocalStudioPalette.current
    val part = instance as? BasePart
    val script = instance as? Script

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlyphBadge(classGlyph(instance.className), classAccent(instance.className), size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        instance.name,
                        color = palette.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(instance.className, color = palette.dim, fontSize = 10.sp)
                }
                StudioIconButton(
                    glyph = Glyph.EDIT,
                    contentDescription = "Rename",
                    onClick = {
                        session.dialog = AppDialog.Prompt(
                            title = "Rename instance",
                            label = "Name",
                            initial = instance.name
                        ) { name ->
                            session.dialog = null
                            if (!name.isNullOrBlank()) controller.rename(instance, name)
                        }
                    },
                    tint = palette.dim,
                    size = 30.dp,
                    iconSize = 15.dp
                )
            }
        }

        item { SectionLabel("General") }
        item {
            PropertyValueRow("Name", instance.name) {
                session.dialog = AppDialog.Prompt(
                    title = "Rename instance",
                    label = "Name",
                    initial = instance.name
                ) { name ->
                    session.dialog = null
                    if (!name.isNullOrBlank()) controller.rename(instance, name)
                }
            }
        }
        item { PropertyValueRow("Parent", instance.parent?.name ?: "—", editable = false) {} }
        item { PropertyValueRow("ClassName", instance.className, editable = false) {} }

        if (part != null) {
            item { SectionLabel("Transform") }
            item {
                VectorProperty(
                    session = session,
                    label = "Position",
                    value = part.cframe.position,
                    onEdit = { axis, value ->
                        val position = part.cframe.position
                        controller.setSelectionPosition(
                            part,
                            when (axis) {
                                0 -> position.copy(x = value)
                                1 -> position.copy(y = value)
                                else -> position.copy(z = value)
                            }
                        )
                    }
                )
            }
            item {
                VectorProperty(
                    session = session,
                    label = "Size",
                    value = part.size,
                    onEdit = { axis, value ->
                        controller.setSelectionSize(part, axis, value)
                    }
                )
            }
            item { SectionLabel("Appearance") }
            item {
                PropertyValueRow("Color", part.color.toHex()) {
                    session.dialog = AppDialog.ColorPick("BrickColor", part.color) { color ->
                        session.dialog = null
                        controller.setPartColor(part, color)
                    }
                }
            }
            item {
                MaterialRow(part.material.name) { material ->
                    controller.setPartMaterial(part, material)
                }
            }
            item {
                ShapeRow(part.shape) { shape -> controller.setShape(part, shape) }
            }
            item {
                TransparencyRow(part.transparency) { value ->
                    controller.setTransparency(part, value)
                }
            }
            item {
                ToggleRow(
                    title = "Anchored",
                    description = "Freeze the part in place",
                    checked = part.anchored
                ) { controller.setAnchored(part, it) }
            }
            item { SectionLabel("Part actions") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioButton("Focus", { controller.focusSelection() }, compact = true, glyph = Glyph.FRAME, modifier = Modifier.weight(1f))
                    StudioButton("Duplicate", { controller.duplicateSelection() }, compact = true, glyph = Glyph.COPY, modifier = Modifier.weight(1f))
                }
            }
        }

        if (script != null) {
            item { SectionLabel("Script") }
            item {
                StudioButton(
                    if (session.activeScript === script) "Script is open" else "Open script editor",
                    onClick = { controller.openScript(script) },
                    glyph = Glyph.CODE,
                    compact = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
            }
            item {
                ToggleRow(
                    title = "Disabled",
                    description = "The script will not run in a playtest",
                    checked = script.disabled
                ) {
                    controller.editProperty(script, "Disabled", script.disabled, it) { value ->
                        script.disabled = value
                    }
                }
            }
            item {
                ToggleRow(
                    title = "Enabled",
                    checked = script.enabled
                ) {
                    controller.editProperty(script, "Enabled", script.enabled, it) { value ->
                        script.enabled = value
                    }
                }
            }
            item {
                RunContextRow(script.runContext.name) { name ->
                    val value = Enums.lookup("RunContext", name) ?: return@RunContextRow
                    controller.editProperty(script, "RunContext", script.runContext, value) {
                        script.runContext = it
                    }
                }
            }
            item {
                val preview = script.source.lineSequence().take(6).joinToString("\n")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.editorBg)
                        .padding(10.dp)
                ) {
                    Text(
                        preview.ifBlank { "-- empty script" },
                        color = palette.editorText.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val humanoid = instance as? Humanoid
        if (humanoid != null) {
            item { SectionLabel("Humanoid") }
            item {
                FloatCommitRow(
                    label = "Health",
                    value = humanoid.health,
                    range = 0f..humanoid.maxHealth.coerceAtLeast(1f),
                    onCommit = {
                        controller.editProperty(humanoid, "Health", humanoid.health, it) { value ->
                            humanoid.health = value
                        }
                    }
                )
            }
            item {
                FloatCommitRow(
                    label = "Walk speed",
                    value = humanoid.walkSpeed,
                    range = 1f..100f,
                    onCommit = {
                        controller.editProperty(humanoid, "WalkSpeed", humanoid.walkSpeed, it) { value ->
                            humanoid.walkSpeed = value
                        }
                    }
                )
            }
            item {
                FloatCommitRow(
                    label = "Jump power",
                    value = humanoid.jumpPower,
                    range = 10f..200f,
                    onCommit = {
                        controller.editProperty(humanoid, "JumpPower", humanoid.jumpPower, it) { value ->
                            humanoid.jumpPower = value
                        }
                    }
                )
            }
        }

        val custom = instance.getProperties()
        if (custom.isNotEmpty()) {
            item { SectionLabel("Custom properties") }
            items(custom.entries.toList(), key = { it.key }) { entry ->
                when (val value = entry.value) {
                    is Boolean -> ToggleRow(
                        title = entry.key,
                        checked = value
                    ) { next ->
                        controller.editProperty(instance, entry.key, value, next) {
                            instance.setProperty(entry.key, it)
                        }
                    }
                    is Float -> PropertyValueRow(entry.key, formatFloat(value)) {
                        session.dialog = AppDialog.Number(
                            title = entry.key,
                            label = "Value",
                            initial = value
                        ) { next ->
                            session.dialog = null
                            if (next != null) {
                                controller.editProperty(instance, entry.key, value, next) {
                                    instance.setProperty(entry.key, it)
                                }
                            }
                        }
                    }
                    is Double -> PropertyValueRow(entry.key, formatFloat(value.toFloat())) {
                        session.dialog = AppDialog.Number(
                            title = entry.key,
                            label = "Value",
                            initial = value.toFloat()
                        ) { next ->
                            session.dialog = null
                            if (next != null) {
                                controller.editProperty(instance, entry.key, value, next.toDouble()) {
                                    instance.setProperty(entry.key, it)
                                }
                            }
                        }
                    }
                    is Int -> PropertyValueRow(entry.key, value.toString()) {
                        session.dialog = AppDialog.Number(
                            title = entry.key,
                            label = "Value",
                            initial = value.toFloat()
                        ) { next ->
                            session.dialog = null
                            if (next != null) {
                                val typed = next.toInt()
                                controller.editProperty(instance, entry.key, value, typed) {
                                    instance.setProperty(entry.key, it)
                                }
                            }
                        }
                    }
                    is String -> PropertyValueRow(entry.key, value, maxLines = 2) {
                        session.dialog = AppDialog.Prompt(
                            title = entry.key,
                            label = "Value",
                            initial = value
                        ) { next ->
                            session.dialog = null
                            if (next != null) {
                                controller.editProperty(instance, entry.key, value, next) {
                                    instance.setProperty(entry.key, it)
                                }
                            }
                        }
                    }
                    is Color3 -> PropertyValueRow(entry.key, value.toHex()) {
                        session.dialog = AppDialog.ColorPick(entry.key, value) { color ->
                            session.dialog = null
                            controller.editProperty(instance, entry.key, value, color) {
                                instance.setProperty(entry.key, it)
                            }
                        }
                    }
                    else -> PropertyValueRow(entry.key, value?.toString() ?: "nil", editable = false) {}
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudioButton(
                    "Duplicate",
                    { controller.duplicateSelection() },
                    compact = true,
                    glyph = Glyph.COPY,
                    modifier = Modifier.weight(1f)
                )
                StudioButton(
                    "Delete",
                    { controller.deleteSelection() },
                    compact = true,
                    glyph = Glyph.TRASH,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PropertyValueRow(
    label: String,
    value: String,
    editable: Boolean = true,
    maxLines: Int = 1,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = editable, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.dim, fontSize = 11.sp, modifier = Modifier.width(88.dp))
        Text(
            value,
            color = if (editable) palette.text else palette.dim,
            fontSize = 11.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (editable) {
            StudioIcon(Glyph.EDIT, palette.dim, size = 12.dp)
        }
    }
}

@Composable
private fun VectorProperty(
    session: StudioSession,
    label: String,
    value: Vector3,
    onEdit: (Int, Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(label, color = palette.dim, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AxisBox("X", value.x, palette.error, 0) {
                askNumber(session, "$label X", value.x) { onEdit(0, it) }
            }
            AxisBox("Y", value.y, palette.play, 1) {
                askNumber(session, "$label Y", value.y) { onEdit(1, it) }
            }
            AxisBox("Z", value.z, palette.info, 2) {
                askNumber(session, "$label Z", value.z) { onEdit(2, it) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AxisBox(
    axis: String,
    value: Float,
    accent: Color,
    index: Int,
    onClick: (Int) -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.input)
            .clickable { onClick(index) }
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(axis, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Text(
            formatFloat(value),
            color = palette.text,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MaterialRow(current: String, onSelect: (com.tetrazbloxy.mobilestudio.model.EnumValue) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalStudioPalette.current
    Box {
        PropertyValueRow("Material", current) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(palette.card)
        ) {
            for (material in Enums.Material.values) {
                DropdownMenuItem(
                    text = {
                        Text(
                            material.name,
                            color = if (material.name == current) palette.accent else palette.text,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(material)
                    }
                )
            }
        }
    }
}

@Composable
private fun ShapeRow(current: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalStudioPalette.current
    val label = Enums.PartType.values.firstOrNull { it.value == current }?.name ?: "Block"
    Box {
        PropertyValueRow("Shape", label) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(palette.card)
        ) {
            for (shape in Enums.PartType.values) {
                DropdownMenuItem(
                    text = {
                        Text(
                            shape.name,
                            color = if (shape.value == current) palette.accent else palette.text,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(shape.value)
                    }
                )
            }
        }
    }
}

@Composable
private fun RunContextRow(current: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val palette = LocalStudioPalette.current
    Box {
        PropertyValueRow("RunContext", current) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(palette.card)
        ) {
            for (context in Enums.RunContext.values) {
                DropdownMenuItem(
                    text = {
                        Text(
                            context.name,
                            color = if (context.name == current) palette.accent else palette.text,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        open = false
                        onSelect(context.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun TransparencyRow(value: Float, onCommit: (Float) -> Unit) {
    val palette = LocalStudioPalette.current
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Transparency", color = palette.dim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("%.2f".format(local), color = palette.accent, fontSize = 10.sp)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.border
            )
        )
    }
}

@Composable
private fun FloatCommitRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = palette.dim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(formatFloat(local), color = palette.accent, fontSize = 10.sp)
        }
        Slider(
            value = local.coerceIn(range.start, range.endInclusive),
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.border
            )
        )
    }
}

private fun formatFloat(value: Float): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    val rounded = kotlin.math.round(value * 100f) / 100f
    return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
}

private fun askNumber(
    session: StudioSession,
    label: String,
    initial: Float,
    onResult: (Float) -> Unit
) {
    session.dialog = AppDialog.Number(title = label, label = "Value", initial = initial) { value ->
        session.dialog = null
        if (value != null) onResult(value)
    }
}
