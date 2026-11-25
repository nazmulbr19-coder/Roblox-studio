package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tetrazbloxy.mobilestudio.core.Zone
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.AppSheet
import com.tetrazbloxy.mobilestudio.studio.PlaceTemplate
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession

private enum class RibbonTab(val title: String) {
    HOME("Home"), MODEL("Model"), UI("UI"), AVATAR("Avatar"),
    PLUGINS("Plugins"), TEST("Test"), VIEW("View")
}

@Composable
fun StudioScreen(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    var fileMenu by remember { mutableStateOf(false) }
    var ribbonTab by remember { mutableStateOf(RibbonTab.HOME) }

    Column(modifier = Modifier.fillMaxSize().background(palette.bg)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(palette.topbar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                StudioButton(
                    label = "File",
                    onClick = { fileMenu = true },
                    compact = true,
                    glyph = Glyph.MENU
                )
                FileMenu(
                    expanded = fileMenu,
                    onDismiss = { fileMenu = false },
                    session = session,
                    controller = controller
                )
            }
            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RibbonTab.entries.forEach { tab ->
                    val active = tab == ribbonTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { ribbonTab = tab }
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Text(
                            tab.title,
                            color = if (active) palette.accent else palette.dim,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))
            Text(
                session.documentTitle,
                color = palette.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = if (session.isPhone) 110.dp else 220.dp)
            )
            Spacer(Modifier.width(4.dp))

            if (!session.isPhone) {
                StudioIconButton(
                    glyph = if (session.dock.getAllHidden()) Glyph.EYE else Glyph.FRAME,
                    contentDescription = "Toggle windows",
                    onClick = {
                        session.setDocksHidden(!session.dock.getAllHidden())
                        session.persistDock()
                    },
                    tint = palette.dim,
                    size = 32.dp,
                    iconSize = 17.dp
                )
            }
            StudioIconButton(
                glyph = Glyph.UNDO,
                contentDescription = "Undo",
                onClick = { controller.undo() },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            StudioIconButton(
                glyph = Glyph.REDO,
                contentDescription = "Redo",
                onClick = { controller.redo() },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (session.playing) palette.stop else palette.play)
                    .clickable { controller.togglePlay() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudioIcon(
                        if (session.playing) Glyph.STOP else Glyph.PLAY,
                        Color.White,
                        size = 15.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (session.playing) "Stop" else "Play",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            AccountChip(session) { session.sheet = AppSheet.ACCOUNT }
        }

        Ribbon(ribbonTab, session, controller)

        if (session.showEditor) {
            ScriptEditorPane(session, controller)
        } else {
            Workspace(session, controller)
        }
    }
}

@Composable
private fun Ribbon(tab: RibbonTab, session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    val groups = when (tab) {
        RibbonTab.HOME -> listOf(
            RibbonGroup("Clipboard", listOf(
                RibbonAction("Cut", Glyph.CUT) { controller.cutSelection() },
                RibbonAction("Copy", Glyph.COPY) { controller.copySelection() },
                RibbonAction("Paste", Glyph.PASTE) { controller.paste() }
            )),
            RibbonGroup("Tools", listOf(
                RibbonAction("Move", Glyph.MOVE) { session.notify("Drag a part to move it") },
                RibbonAction("Rotate", Glyph.ROTATE) { controller.rotateSelection(15f) },
                RibbonAction("Scale", Glyph.SCALE) { controller.scaleSelection(1.25f) }
            )),
            RibbonGroup("Insert", listOf(
                RibbonAction("Part", Glyph.CUBE) { controller.newPart() },
                RibbonAction("Script", Glyph.SCRIPT) { controller.newScript() },
                RibbonAction("Folder", Glyph.FOLDER) { controller.newFolder() }
            )),
            RibbonGroup("Edit", listOf(
                RibbonAction("Duplicate", Glyph.COPY) { controller.duplicateSelection() },
                RibbonAction("Delete", Glyph.TRASH) { controller.deleteSelection() },
                RibbonAction("Group", Glyph.LAYERS) { controller.groupSelection() }
            )),
            RibbonGroup("Snap", listOf(
                RibbonAction("Move", Glyph.SNAP) {
                    session.settings.moveSnapOn = !session.settings.moveSnapOn
                    session.notify("Move snap ${if (session.settings.moveSnapOn) "on" else "off"}")
                },
                RibbonAction("Rotate", Glyph.ROTATE) {
                    session.settings.rotSnapOn = !session.settings.rotSnapOn
                    session.notify("Rotate snap ${if (session.settings.rotSnapOn) "on" else "off"}")
                }
            ))
        )
        RibbonTab.MODEL -> listOf(
            RibbonGroup("Parts", listOf(
                RibbonAction("Part", Glyph.CUBE) { controller.newPart() },
                RibbonAction("Sphere", Glyph.CUBE) {
                    controller.newPart(com.tetrazbloxy.mobilestudio.model.Enums.PartType.SPHERE.value)
                },
                RibbonAction("Wedge", Glyph.SHAPES) {
                    controller.insertClass("WedgePart")
                },
                RibbonAction("Spawn", Glyph.SPAWN) { controller.insertClass("SpawnLocation") }
            )),
            RibbonGroup("Appearance", listOf(
                RibbonAction("Color", Glyph.PALETTE) { openColorForSelection(session, controller) },
                RibbonAction("Material", Glyph.LIGHT) { session.notify("Use Properties to change Material") },
                RibbonAction("Anchor", Glyph.LOCK) {
                    session.selectedPart?.let { controller.setAnchored(it, !it.anchored) }
                }
            )),
            RibbonGroup("Solid Modeling", listOf(
                RibbonAction("Union", Glyph.LAYERS) { controller.solidOperation("Union") },
                RibbonAction("Negate", Glyph.MINUS) { controller.solidOperation("Negate") },
                RibbonAction("Separate", Glyph.SCALE) { controller.solidOperation("Separate") }
            )),
            RibbonGroup("Constraints", listOf(
                RibbonAction("Weld", Glyph.WELD) { controller.weldSelection() }
            )),
            RibbonGroup("Selection", listOf(
                RibbonAction("Select All", Glyph.CHECK) { controller.selectAll() },
                RibbonAction("Clear", Glyph.CLOSE) { controller.clearSelection() }
            ))
        )
        RibbonTab.UI -> listOf(
            RibbonGroup("Containers", listOf(
                RibbonAction("ScreenGui", Glyph.GUI) { controller.newGui("ScreenGui") },
                RibbonAction("Frame", Glyph.GUI) { controller.newGui("Frame") },
                RibbonAction("Billboard", Glyph.GUI) { controller.newGui("BillboardGui") }
            )),
            RibbonGroup("Text & Images", listOf(
                RibbonAction("TextLabel", Glyph.TEXT) { controller.newGui("TextLabel") },
                RibbonAction("TextButton", Glyph.TEXT) { controller.newGui("TextButton") },
                RibbonAction("ImageLabel", Glyph.IMAGE) { controller.newGui("ImageLabel") }
            )),
            RibbonGroup("Layout", listOf(
                RibbonAction("Folder", Glyph.FOLDER) { controller.newFolder() }
            ))
        )
        RibbonTab.AVATAR -> listOf(
            RibbonGroup("Game", listOf(
                RibbonAction("Avatar", Glyph.ACCOUNT) { session.sheet = AppSheet.AVATAR },
                RibbonAction("Settings", Glyph.SETTINGS) { session.sheet = AppSheet.SETTINGS }
            )),
            RibbonGroup("Playtest", listOf(
                RibbonAction("Play", Glyph.PLAY) { controller.startPlay() },
                RibbonAction("Stop", Glyph.STOP) { controller.stopPlay() }
            ))
        )
        RibbonTab.PLUGINS -> listOf(
            RibbonGroup("Plugins", listOf(
                RibbonAction("Manager", Glyph.PLUGIN) { session.sheet = AppSheet.PLUGINS },
                RibbonAction("From Roblox", Glyph.DOWNLOAD) {
                    session.dialog = AppDialog.Prompt(
                        title = "Install plugin from Roblox",
                        label = "Asset ID or link"
                    ) { input ->
                        session.dialog = null
                        if (!input.isNullOrBlank()) controller.installPluginFromRoblox(input)
                    }
                },
                RibbonAction("From file", Glyph.FILE) { session.requestOpenPlugin?.invoke() }
            ))
        )
        RibbonTab.TEST -> listOf(
            RibbonGroup("Test", listOf(
                RibbonAction("Play", Glyph.PLAY) { controller.startPlay() },
                RibbonAction("Stop", Glyph.STOP) { controller.stopPlay() }
            )),
            RibbonGroup("Output", listOf(
                RibbonAction("Show", Glyph.TERMINAL) {
                    session.dock.panel("output")?.let { panel ->
                        if (!panel.visible) session.toggleDock("output")
                    }
                },
                RibbonAction("Clear", Glyph.TRASH) { session.output.clear() }
            ))
        )
        RibbonTab.VIEW -> listOf(
            RibbonGroup("Windows", listOf(
                RibbonAction("Explorer", Glyph.LAYERS) { showDock(session, "explorer") },
                RibbonAction("Properties", Glyph.SETTINGS) { showDock(session, "properties") },
                RibbonAction("Toolbox", Glyph.CHECK) { showDock(session, "toolbox") },
                RibbonAction("Output", Glyph.TERMINAL) { showDock(session, "output") }
            )),
            RibbonGroup("Viewport", listOf(
                RibbonAction("Grid", Glyph.GRID) {
                    session.showGrid = !session.showGrid
                    session.touchScene()
                },
                RibbonAction("Focus", Glyph.FRAME) { controller.focusSelection() }
            )),
            RibbonGroup("Studio", listOf(
                RibbonAction("Settings", Glyph.SETTINGS) { session.sheet = AppSheet.SETTINGS },
                RibbonAction("About", Glyph.HELP) { session.sheet = AppSheet.ABOUT }
            ))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.ribbon)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (group in groups) {
                Column {
                    Text(
                        group.title,
                        color = palette.dim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (action in group.actions) {
                            RibbonButton(
                                label = action.label,
                                glyph = action.glyph,
                                onClick = action.onClick
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RibbonGroup(val title: String, val actions: List<RibbonAction>)
private data class RibbonAction(val label: String, val glyph: Glyph, val onClick: () -> Unit)

private fun showDock(session: StudioSession, id: String) {
    val panel = session.dock.panel(id) ?: return
    if (!panel.visible) session.toggleDock(id)
}

private fun openColorForSelection(session: StudioSession, controller: StudioController) {
    val part = session.selectedPart
    if (part == null) {
        session.notify("Select a part to change its color")
        return
    }
    session.dialog = AppDialog.ColorPick("BrickColor", part.color) { color ->
        session.dialog = null
        controller.setPartColor(part, color)
    }
}

@Composable
private fun Workspace(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    session.dockVersion

    val leftVisible = session.dock.panels.filter { it.zone == Zone.LEFT && it.visible }
    val rightVisible = session.dock.panels.filter { it.zone == Zone.RIGHT && it.visible }
    val bottomVisible = session.dock.panels.filter { it.zone == Zone.BOTTOM && it.visible }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (leftVisible.isNotEmpty()) {
                DockZone(
                    session = session,
                    controller = controller,
                    ids = leftVisible.map { it.id },
                    width = session.dock.leftW.dp,
                    zone = "left"
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                Viewport(session, controller)
            }
            if (rightVisible.isNotEmpty()) {
                DockZone(
                    session = session,
                    controller = controller,
                    ids = rightVisible.map { it.id },
                    width = session.dock.rightW.dp,
                    zone = "right"
                )
            }
        }
        if (bottomVisible.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(session.dock.bottomH.dp)
            ) {
                DockZone(
                    session = session,
                    controller = controller,
                    ids = bottomVisible.map { it.id },
                    width = null,
                    zone = "bottom"
                )
            }
        }
    }
}

@Composable
private fun DockZone(
    session: StudioSession,
    controller: StudioController,
    ids: List<String>,
    width: androidx.compose.ui.unit.Dp?,
    zone: String
) {
    val palette = LocalStudioPalette.current
    var selected by remember(ids.joinToString()) { mutableStateOf(ids.firstOrNull() ?: "") }
    val current = ids.firstOrNull { it == selected } ?: ids.firstOrNull() ?: return

    val modifier = if (width != null) {
        Modifier.width(width).fillMaxHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Column(modifier = modifier.background(palette.panel)) {
        if (ids.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(palette.header)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (id in ids) {
                    val active = id == current
                    val label = when (id) {
                        "explorer" -> "Explorer"
                        "properties" -> "Properties"
                        "toolbox" -> "Toolbox"
                        "output" -> "Output"
                        else -> id.replaceFirstChar { it.uppercase() }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (active) palette.accent.copy(alpha = 0.18f) else Color.Transparent)
                            .clickable { selected = id }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            color = if (active) palette.accent else palette.dim,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (current) {
                "explorer" -> ExplorerPanel(session, controller)
                "properties" -> PropertiesPanel(session, controller)
                "toolbox" -> ToolboxPanel(session, controller)
                "output" -> OutputPanel(session, controller)
            }
        }
    }
}

@Composable
private fun FileMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    session: StudioSession,
    controller: StudioController
) {
    val palette = LocalStudioPalette.current
    val cloud = session.cloudPlace
    val entries = buildList {
        add("Home (Start Page)" to {
            controller.guardDiscard { session.showStart() }
        })
        add("New Place" to { controller.newPlace(PlaceTemplate.BASEPLATE) })
        add("Open File… (.rbxl / .rbxm)" to { session.requestOpenFile?.invoke() })
        add("Open from Roblox (My Games)…" to { session.showStart() })
        if (cloud != null) {
            add("Save to Roblox" to { controller.saveToRoblox() })
            add("Save a Copy to This Device…" to { controller.saveAs() })
            add("Publish to Roblox (${cloud.name})" to { controller.saveToRoblox() })
            add("Publish to Roblox As…" to { controller.publishAs() })
            add("Download / Version History (${cloud.name})…" to {
                controller.showVersionHistory(cloud)
            })
        } else {
            add("Save" to { controller.save() })
            add("Save As…" to { controller.saveAs() })
            add("Publish to Roblox…" to { controller.publishAs() })
            add("Publish to Roblox As…" to { controller.publishAs() })
            add("Download Game / Old Version…" to {
                session.dialog = AppDialog.Message(
                    "Download game",
                    "Open a game from My Games on the Start page to download it or view its versions."
                )
            })
        }
        add("Export Selection (.rbxm)…" to { controller.exportSelection() })
        add("Import Model into Toolbox…" to { session.requestImportModel?.invoke() })
        add("Install Plugin from Roblox (ID or link)…" to {
            session.dialog = AppDialog.Prompt(
                title = "Install plugin from Roblox",
                label = "Asset ID or link"
            ) { input ->
                session.dialog = null
                if (!input.isNullOrBlank()) controller.installPluginFromRoblox(input)
            }
        })
        add("Install Plugin from File…" to { session.requestOpenPlugin?.invoke() })
        add("Playtest Avatar…" to { session.sheet = AppSheet.AVATAR })
        add("Game Settings (Avatar: R6 / R15)…" to { session.sheet = AppSheet.SETTINGS })
        add("Studio Settings…" to { session.sheet = AppSheet.SETTINGS })
        add(
            if (session.accounts.signedIn) {
                "Roblox Account (@${session.accounts.userName.ifBlank { "signed in" }})…"
            } else {
                "Sign in to Roblox…"
            } to { session.sheet = AppSheet.ACCOUNT }
        )
        add("About Mobile Studio (v2.5)" to { session.sheet = AppSheet.ABOUT })
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(min = 270.dp, max = 380.dp)
            .background(palette.card)
    ) {
        for ((label, action) in entries) {
            DropdownMenuItem(
                text = {
                    Text(label, color = palette.text, fontSize = 12.sp, maxLines = 1)
                },
                onClick = {
                    onDismiss()
                    action()
                }
            )
        }
    }
}
