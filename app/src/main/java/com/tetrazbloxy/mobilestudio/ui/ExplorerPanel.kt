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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession

private data class TreeRow(val instance: Instance, val depth: Int)

@Composable
fun ExplorerPanel(
    session: StudioSession,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var query by remember { mutableStateOf("") }
    var multi by remember { mutableStateOf(false) }
    var insertOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val expanded = remember(session.document) {
        mutableStateMapOf<Instance, Boolean>().apply {
            val workspace = session.document.workspace
            put(workspace, true)
        }
    }

    val rows = buildList {
        if (query.isNotBlank()) {
            val text = query.trim()
            for (node in session.document.getDescendants()) {
                if (node.name.contains(text, true) || node.className.contains(text, true)) {
                    add(TreeRow(node, 0))
                }
            }
        } else {
            fun walk(parent: Instance, depth: Int) {
                for (child in parent.children) {
                    add(TreeRow(child, depth))
                    if (expanded[child] == true) walk(child, depth + 1)
                }
            }
            walk(session.document, 0)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(palette.panel)) {
        PanelHeader("Explorer", Glyph.LAYERS) {
            Box {
                StudioIconButton(
                    glyph = Glyph.PLUS,
                    contentDescription = "Insert",
                    onClick = { insertOpen = true },
                    tint = palette.accent,
                    size = 30.dp,
                    iconSize = 17.dp
                )
                DropdownMenu(
                    expanded = insertOpen,
                    onDismissRequest = { insertOpen = false },
                    modifier = Modifier.background(palette.card)
                ) {
                    InsertMenuItem("Part", Glyph.CUBE) { controller.newPart(); insertOpen = false }
                    InsertMenuItem("Script", Glyph.SCRIPT) { controller.newScript("Script"); insertOpen = false }
                    InsertMenuItem("LocalScript", Glyph.CODE) { controller.newScript("LocalScript"); insertOpen = false }
                    InsertMenuItem("ModuleScript", Glyph.FILE) { controller.newScript("ModuleScript"); insertOpen = false }
                    InsertMenuItem("Folder", Glyph.FOLDER) { controller.newFolder(); insertOpen = false }
                    InsertMenuItem("Model", Glyph.LAYERS) { controller.newModel(); insertOpen = false }
                    InsertMenuItem("Choose class…", Glyph.SEARCH) {
                        insertOpen = false
                        session.dialog = AppDialog.PickClass { className ->
                            session.dialog = null
                            if (className != null) controller.insertClass(className)
                        }
                    }
                }
            }
            StudioIconButton(
                glyph = Glyph.LAYERS,
                contentDescription = "Multi-select",
                onClick = { multi = !multi },
                tint = if (multi) palette.accent else palette.dim,
                size = 30.dp,
                iconSize = 16.dp
            )
            Box {
                StudioIconButton(
                    glyph = Glyph.MORE,
                    contentDescription = "Explorer actions",
                    onClick = { moreOpen = true },
                    tint = palette.dim,
                    size = 30.dp,
                    iconSize = 16.dp
                )
                DropdownMenu(
                    expanded = moreOpen,
                    onDismissRequest = { moreOpen = false },
                    modifier = Modifier.background(palette.card)
                ) {
                    DropdownMenuItem(
                        text = { Text("Expand all", color = palette.text, fontSize = 13.sp) },
                        onClick = {
                            moreOpen = false
                            for (node in session.document.getDescendants()) {
                                if (node.children.isNotEmpty()) expanded[node] = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Collapse all", color = palette.text, fontSize = 13.sp) },
                        onClick = {
                            moreOpen = false
                            expanded.clear()
                            expanded[session.document.workspace] = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reparent selection to first", color = palette.text, fontSize = 13.sp) },
                        enabled = session.selection.size >= 2,
                        onClick = {
                            moreOpen = false
                            val target = session.selection.firstOrNull() ?: return@DropdownMenuItem
                            session.selection.drop(1).forEach { controller.reparent(it, target) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Close Explorer", color = palette.text, fontSize = 13.sp) },
                        onClick = {
                            moreOpen = false
                            session.toggleDock("explorer")
                        }
                    )
                }
            }
            StudioIconButton(
                glyph = Glyph.CLOSE,
                contentDescription = "Close Explorer",
                onClick = { session.toggleDock("explorer") },
                tint = palette.dim,
                size = 30.dp,
                iconSize = 15.dp
            )
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Filter instances",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)
        )

        if (rows.isEmpty()) {
            EmptyState(
                glyph = Glyph.LAYERS,
                title = if (query.isBlank()) "No instances" else "No matches",
                subtitle = if (query.isBlank()) {
                    "Insert a part or script from the + button."
                } else {
                    "Try a different name or class."
                }
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { it.instance }) { row ->
                    ExplorerRow(
                        session = session,
                        controller = controller,
                        instance = row.instance,
                        depth = row.depth,
                        expanded = expanded[row.instance] == true,
                        onToggleExpand = {
                            expanded[row.instance] = !(expanded[row.instance] ?: false)
                        },
                        multi = multi,
                        query = query
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun InsertMenuItem(
    label: String,
    glyph: Glyph,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    DropdownMenuItem(
        leadingIcon = { StudioIcon(glyph, palette.accent, size = 18.dp) },
        text = { Text(label, color = palette.text, fontSize = 13.sp) },
        onClick = onClick
    )
}

@Composable
private fun ExplorerRow(
    session: StudioSession,
    controller: StudioController,
    instance: Instance,
    depth: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    multi: Boolean,
    query: String
) {
    val palette = LocalStudioPalette.current
    val selected = session.selection.any { it === instance }
    var menu by remember(instance) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) palette.select else Color.Transparent)
            .clickable {
                if (multi) {
                    session.toggleSelection(instance)
                } else {
                    session.setSelection(listOf(instance))
                }
            }
            .padding(
                start = 6.dp + (depth.coerceAtMost(12) * 13).dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clickable(enabled = instance.children.isNotEmpty()) {
                    onToggleExpand()
                },
            contentAlignment = Alignment.Center
        ) {
            if (instance.children.isNotEmpty()) {
                StudioIcon(
                    if (expanded) Glyph.CHEVRON_DOWN else Glyph.CHEVRON_RIGHT,
                    palette.dim,
                    size = 13.dp
                )
            }
        }
        StudioIcon(
            classGlyph(instance.className),
            classAccent(instance.className),
            size = 15.dp
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                instance.name.ifBlank { instance.className },
                color = if (selected) palette.text else palette.text.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (query.isNotBlank()) {
                Text(
                    instance.className,
                    color = palette.dim,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
        if (instance is Script) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .clickable { controller.openScript(instance) },
                contentAlignment = Alignment.Center
            ) {
                StudioIcon(Glyph.CODE, palette.dim, size = 12.dp)
            }
        }
        Box {
            StudioIconButton(
                glyph = Glyph.MORE,
                contentDescription = "Instance actions",
                onClick = { menu = true },
                tint = palette.dim,
                size = 24.dp,
                iconSize = 13.dp
            )
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                modifier = Modifier.background(palette.card)
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", color = palette.text, fontSize = 13.sp) },
                    onClick = {
                        menu = false
                        session.dialog = AppDialog.Prompt(
                            title = "Rename instance",
                            label = "Name",
                            initial = instance.name
                        ) { name ->
                            session.dialog = null
                            if (!name.isNullOrBlank()) controller.rename(instance, name)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate", color = palette.text, fontSize = 13.sp) },
                    onClick = {
                        menu = false
                        session.setSelection(listOf(instance))
                        controller.duplicateSelection()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = palette.error, fontSize = 13.sp) },
                    onClick = {
                        menu = false
                        session.setSelection(listOf(instance))
                        controller.deleteSelection()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Focus in viewport", color = palette.text, fontSize = 13.sp) },
                    onClick = {
                        menu = false
                        session.setSelection(listOf(instance))
                        controller.focusSelection()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move to Workspace", color = palette.text, fontSize = 13.sp) },
                    enabled = instance.parent !== session.document.workspace,
                    onClick = {
                        menu = false
                        controller.reparent(instance, session.document.workspace)
                    }
                )
                if (instance is Script) {
                    DropdownMenuItem(
                        text = { Text("Open script", color = palette.text, fontSize = 13.sp) },
                        onClick = {
                            menu = false
                            controller.openScript(instance)
                        }
                    )
                }
                if (instance is BasePart) {
                    DropdownMenuItem(
                        text = { Text("Select in Properties", color = palette.text, fontSize = 13.sp) },
                        onClick = {
                            menu = false
                            session.setSelection(listOf(instance))
                        }
                    )
                }
            }
        }
    }
}
