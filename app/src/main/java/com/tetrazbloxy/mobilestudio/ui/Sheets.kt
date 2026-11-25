package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.audio.AudioEngine
import com.tetrazbloxy.mobilestudio.core.Theme
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.AppSheet
import com.tetrazbloxy.mobilestudio.studio.AudioItem
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import com.tetrazbloxy.mobilestudio.studio.ToolboxCatalog
import kotlinx.coroutines.launch

@Composable
fun SheetHost(session: StudioSession, controller: StudioController, sheet: AppSheet) {
    val close = { session.sheet = null }
    when (sheet) {
        AppSheet.FILE_MENU -> Unit
        AppSheet.SETTINGS -> SettingsSheet(session, close)
        AppSheet.ACCOUNT -> AccountSheet(session, controller, close)
        AppSheet.PLUGINS -> PluginsSheet(session, controller, close)
        AppSheet.ABOUT -> AboutSheet(close)
        AppSheet.AVATAR -> AvatarSheet(session, close)
        AppSheet.AUDIO -> AudioSheet(session, controller, close)
    }
}

@Composable
private fun SettingsSheet(session: StudioSession, onClose: () -> Unit) {
    val settings = session.settings
    StudioSheet(title = "Studio Settings", subtitle = "Preferences are saved on this device", onClose = onClose) {
        SectionLabel("Appearance")
        val themes = remember(session.themeVersion) {
            Theme.ALL + settings.customThemes()
        }
        var themeMenu by remember { mutableStateOf(false) }
        SelectRow(
            title = "Theme",
            value = settings.themeName,
            onClick = { themeMenu = true }
        )
        if (themeMenu) {
            StudioDropdown(
                expanded = themeMenu,
                onDismiss = { themeMenu = false },
                items = themes.map { it.name },
                selected = settings.themeName,
                onSelect = { session.applyTheme(it) }
            )
        }
        StudioButton(
            "Create custom theme",
            onClick = {
                session.dialog = AppDialog.Prompt(
                    title = "New custom theme",
                    label = "Name",
                    initial = "Custom ${settings.customThemes().size + 1}"
                ) { name ->
                    session.dialog = null
                    if (name.isNullOrBlank()) return@Prompt
                    val base = session.theme
                    val custom = base.copy(name = name.trim(), custom = true)
                    val list = settings.customThemes() + custom
                    settings.saveCustomThemes(list)
                    session.applyTheme(custom.name)
                }
            },
            compact = true,
            glyph = Glyph.PALETTE
        )
        if (session.theme.custom) {
            StudioButton(
                "Edit accent color",
                onClick = {
                    session.dialog = AppDialog.ColorPick(
                        "Theme accent",
                        com.tetrazbloxy.mobilestudio.model.Color3.fromArgb(session.theme.accent)
                    ) { color ->
                        session.dialog = null
                        val list = settings.customThemes().toMutableList()
                        val index = list.indexOfFirst { it.name == session.theme.name }
                        if (index >= 0) {
                            list[index] = list[index].copy(accent = color.toArgb())
                            settings.saveCustomThemes(list)
                            session.applyTheme(list[index].name)
                        }
                    }
                },
                compact = true,
                glyph = Glyph.PALETTE
            )
            StudioButton(
                "Delete custom theme",
                onClick = {
                    val list = settings.customThemes().filterNot { it.name == session.theme.name }
                    settings.saveCustomThemes(list)
                    session.applyTheme(Theme.DARK.name)
                },
                compact = true,
                glyph = Glyph.TRASH
            )
        }

        SectionLabel("Script editor")
        ToggleRow("Auto-close brackets", "Insert a matching bracket or quote", settings.autoCloseBrackets) {
            settings.autoCloseBrackets = it
        }
        ToggleRow("Auto indent", "Keep the previous line's indentation", settings.autoIndent) {
            settings.autoIndent = it
        }
        ToggleRow("Autocomplete", "Suggest Lua keywords and members", settings.autocomplete) {
            settings.autocomplete = it
        }
        SliderRow(
            title = "Editor font size",
            value = settings.editorFontSize,
            onValueChange = { settings.editorFontSize = it },
            valueRange = 9f..26f,
            valueLabel = { "${it.toInt()} sp" }
        )

        SectionLabel("Editing")
        ToggleRow("Confirm delete", "Ask before deleting instances", settings.confirmDelete) {
            settings.confirmDelete = it
        }
        ToggleRow("Move snap", "Snap movement to the grid", settings.moveSnapOn) {
            settings.moveSnapOn = it
        }
        SliderRow("Move snap interval", settings.moveSnap, { settings.moveSnap = it }, 0.25f..16f)
        ToggleRow("Rotate snap", "Snap rotation to fixed angles", settings.rotSnapOn) {
            settings.rotSnapOn = it
        }
        SliderRow(
            "Rotate snap interval", settings.rotSnap, { settings.rotSnap = it }, 1f..90f,
            valueLabel = { "${it.toInt()}°" }
        )
        ToggleRow("Drag collisions", "Collide while dragging parts", settings.dragCollisions) {
            settings.dragCollisions = it
        }
        ToggleRow("Select inside models", "Pick parts inside models", settings.selectInsideModels) {
            settings.selectInsideModels = it
        }

        SectionLabel("Playtest")
        ToggleRow("Show FPS", "Display the live frame rate", settings.showFps) {
            settings.showFps = it
        }
        ToggleRow("Hide windows in playtest", "Hide docks while playing", settings.hideWindowsInPlaytest) {
            settings.hideWindowsInPlaytest = it
        }
        SliderRow(
            "Play camera sensitivity", settings.playCameraSensitivity,
            { settings.playCameraSensitivity = it }, 0.2f..3f
        )
        SliderRow("Fly speed", settings.flySpeed, { settings.flySpeed = it }, 0.25f..8f)
        var cameraMenu by remember { mutableStateOf(false) }
        SelectRow("Camera mode", settings.cameraMode, { cameraMenu = true })
        if (cameraMenu) {
            StudioDropdown(
                expanded = cameraMenu,
                onDismiss = { cameraMenu = false },
                items = listOf("Follow", "Default", "Classic", "First Person"),
                selected = settings.cameraMode,
                onSelect = { settings.cameraMode = it }
            )
        }
        var movementMenu by remember { mutableStateOf(false) }
        SelectRow("Movement mode", settings.movementMode, { movementMenu = true })
        if (movementMenu) {
            StudioDropdown(
                expanded = movementMenu,
                onDismiss = { movementMenu = false },
                items = listOf("Dynamic Thumbstick", "Fixed Thumbstick", "Buttons"),
                selected = settings.movementMode,
                onSelect = { settings.movementMode = it }
            )
        }
        ToggleRow("Shift lock switch", "Allow toggling shift lock", settings.shiftLockSwitch) {
            settings.shiftLockSwitch = it
        }

        SectionLabel("Graphics & audio")
        SliderRow(
            "Graphics quality",
            settings.graphicsQuality.toFloat(),
            { settings.graphicsQuality = it.toInt() },
            1f..10f,
            valueLabel = { it.toInt().toString() }
        )
        var renderMenu by remember { mutableStateOf(false) }
        SelectRow("Render distance", settings.renderDistance, { renderMenu = true })
        if (renderMenu) {
            StudioDropdown(
                expanded = renderMenu,
                onDismiss = { renderMenu = false },
                items = listOf("Automatic", "Short", "Medium", "Long"),
                selected = settings.renderDistance,
                onSelect = { settings.renderDistance = it }
            )
        }
        ToggleRow("Modern materials", "Use the 2022 material lookup", settings.modernMaterials) {
            settings.modernMaterials = it
        }
        SliderRow(
            "Master volume",
            settings.volume.toFloat(),
            { value ->
                settings.volume = value.toInt()
                session.audio.setMasterVolume(value / 10f)
            },
            0f..10f,
            valueLabel = { "${it.toInt()}" }
        )
        SliderRow(
            "Plugin UI scale",
            settings.pluginUiScale,
            { settings.pluginUiScale = it },
            0f..2f
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AccountSheet(
    session: StudioSession,
    controller: StudioController,
    onClose: () -> Unit
) {
    val accounts = session.accounts
    StudioSheet(
        title = "Roblox Account",
        subtitle = if (accounts.signedIn) {
            "Signed in as @${accounts.userName.ifBlank { "unknown" }}"
        } else {
            "Sign in with a cookie or Open Cloud API key"
        },
        onClose = onClose
    ) {
        if (!accounts.signedIn) {
            var credential by remember { mutableStateOf("") }
            Text("Cookie or API key", color = LocalStudioPalette.current.dim, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp))
            Spacer(Modifier.height(6.dp))
            StudioField(
                value = credential,
                onValueChange = { credential = it },
                placeholder = ".ROBLOSECURITY=… or API key",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            )
            Spacer(Modifier.height(10.dp))
            StudioButton(
                "Sign in",
                onClick = {
                    if (credential.isBlank()) {
                        session.notify("Enter a cookie or API key")
                        return@StudioButton
                    }
                    session.scope.launch {
                        val result = accounts.signIn(credential)
                        session.notify(result.message)
                        if (result.success) {
                            session.startCache.load(
                                com.tetrazbloxy.mobilestudio.ui.StartCache.Kind.MINE,
                                accounts.userId
                            )
                        }
                    }
                },
                primary = true,
                glyph = Glyph.ACCOUNT,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            )
        } else {
            KeyValueLine("User ID", accounts.userId.toString())
            KeyValueLine("Display name", accounts.displayName.ifBlank { accounts.userName })
            KeyValueLine("Username", "@${accounts.userName}")
            KeyValueLine("API key", if (accounts.apiKey.isBlank()) "Not set" else "Configured")
            Spacer(Modifier.height(8.dp))
            StudioButton("My Games", {
                onClose()
                session.showStart()
            }, glyph = Glyph.CUBE, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            StudioButton("Save to Roblox", {
                onClose()
                controller.saveToRoblox()
            }, glyph = Glyph.SAVE, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            StudioButton("Publish to Roblox As…", {
                onClose()
                controller.publishAs()
            }, glyph = Glyph.UPLOAD, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            StudioButton("Install plugin from Roblox", {
                session.dialog = AppDialog.Prompt(
                    title = "Install plugin from Roblox",
                    label = "Asset ID or link"
                ) { input ->
                    session.dialog = null
                    if (!input.isNullOrBlank()) controller.installPluginFromRoblox(input)
                }
            }, glyph = Glyph.PLUGIN, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
            Spacer(Modifier.height(8.dp))
            val saved = accounts.accounts()
            if (saved.size > 1) {
                SectionLabel("Saved accounts")
                for (account in saved) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (accounts.switchTo(account)) {
                                    session.notify("Switched to @${account.name}")
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlyphBadge(Glyph.ACCOUNT, LocalStudioPalette.current.accent, size = 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "@${account.name}",
                                color = LocalStudioPalette.current.text,
                                fontSize = 12.sp
                            )
                            Text(
                                account.displayName,
                                color = LocalStudioPalette.current.dim,
                                fontSize = 9.sp
                            )
                        }
                        if (account.id == accounts.userId) TagChip("ACTIVE", LocalStudioPalette.current.play)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            StudioButton("Sign out", {
                accounts.signOut()
                session.notify("Signed out")
            }, glyph = Glyph.LOGOUT, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        }

        SectionLabel("Web permissions")
        val remembered = remember { session.webPermissions.remembered() }
        if (remembered.isEmpty()) {
            Text(
                "No web permissions have been remembered.",
                color = LocalStudioPalette.current.dim,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        } else {
            for ((host, allowed) in remembered) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(host, color = LocalStudioPalette.current.text, fontSize = 11.sp,
                        modifier = Modifier.weight(1f))
                    StatusPill(
                        if (allowed) "Allowed" else "Blocked",
                        if (allowed) LocalStudioPalette.current.play else LocalStudioPalette.current.stop
                    )
                }
            }
            StudioButton("Forget all", {
                session.webPermissions.forgetAll()
                session.notify("Web permissions cleared")
            }, glyph = Glyph.TRASH, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PluginsSheet(
    session: StudioSession,
    controller: StudioController,
    onClose: () -> Unit
) {
    StudioSheet(
        title = "Plugin Manager",
        subtitle = "Plugins are detected and listed; Lua execution is not enabled in this build.",
        onClose = onClose
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioButton(
                "From Roblox…",
                onClick = {
                    session.dialog = AppDialog.Prompt(
                        title = "Install plugin from Roblox",
                        label = "Asset ID or link"
                    ) { input ->
                        session.dialog = null
                        if (!input.isNullOrBlank()) {
                            controller.installPluginFromRoblox(input)
                        }
                    }
                },
                compact = true,
                glyph = Glyph.DOWNLOAD,
                modifier = Modifier.weight(1f)
            )
            StudioButton(
                "From file…",
                onClick = { session.requestOpenPlugin?.invoke() },
                compact = true,
                glyph = Glyph.FILE,
                modifier = Modifier.weight(1f)
            )
            StudioButton(
                "Reload",
                onClick = { session.pluginHost.loadAll() },
                compact = true,
                glyph = Glyph.REFRESH,
                modifier = Modifier.weight(1f)
            )
        }
        if (session.pluginHost.entries.isEmpty()) {
            EmptyState(
                glyph = Glyph.PLUGIN,
                title = "No plugins installed",
                subtitle = "Install a .rbxm, .rbxmx, .lua or .luau plugin to see it here.",
            )
        } else {
            for (entry in session.pluginHost.entries.toList()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlyphBadge(
                        if (entry.kind?.extension == "lua") Glyph.CODE else Glyph.PLUGIN,
                        if (entry.error != null) LocalStudioPalette.current.error
                        else LocalStudioPalette.current.accent,
                        size = 34.dp
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, color = LocalStudioPalette.current.text, fontSize = 12.sp)
                        Text(
                            when {
                                entry.error != null -> entry.error
                                session.pluginHost.activePlugin == entry.name -> "Active"
                                else -> "${entry.kind?.extension?.uppercase() ?: "?"} · ${entry.scriptCount} script(s)"
                            },
                            color = if (entry.error != null) LocalStudioPalette.current.error
                            else LocalStudioPalette.current.dim,
                            fontSize = 9.sp
                        )
                    }
                    SwitchCompat(entry.enabled) {
                        session.pluginHost.setEnabled(entry, it)
                        if (it) session.pluginHost.loadAll() else session.pluginHost.deactivate()
                    }
                    StudioIconButton(
                        glyph = Glyph.TRASH,
                        contentDescription = "Uninstall ${entry.name}",
                        onClick = { session.pluginHost.remove(entry) },
                        tint = LocalStudioPalette.current.dim,
                        size = 30.dp,
                        iconSize = 15.dp
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SwitchCompat(checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = LocalStudioPalette.current.accent,
            uncheckedThumbColor = LocalStudioPalette.current.dim,
            uncheckedTrackColor = LocalStudioPalette.current.input
        )
    )
}

@Composable
private fun AboutSheet(onClose: () -> Unit) {
    val palette = LocalStudioPalette.current
    StudioSheet(title = "About Mobile Studio", subtitle = "Version 2.5 (build 8)", onClose = onClose) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                StudioIcon(Glyph.CUBE, Color.White, size = 42.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text("Mobile Studio", color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("A place editor for Android", color = palette.dim, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            LinkRow("Downloads", "https://www.mediafire.com/folder/33xwlczg9fxs1/mobile+studio")
            LinkRow("YouTube", "https://www.youtube.com/@fiowwwys/featured")
            Spacer(Modifier.height(14.dp))
            Text(
                "XML RBXLX and RBXMX documents are fully editable. Binary bodies are detected and validated only. " +
                    "Lua plugin scripts are discovered but are not executed in this build.",
                color = palette.dim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .clickable { /* external links are informational only */ }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(Glyph.LINK, palette.accent, size = 16.dp)
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = palette.text, fontSize = 12.sp)
            Text(url, color = palette.dim, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AvatarSheet(session: StudioSession, onClose: () -> Unit) {
    StudioSheet(
        title = "Playtest Avatar",
        subtitle = "Choose how your character looks during a playtest",
        onClose = onClose
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            AvatarKindPicker(session.avatarKind) { session.avatarKind = it }
            Spacer(Modifier.height(14.dp))
            ToggleRow(
                "Morph to Player's Choice",
                "Use the signed-in account's avatar",
                session.morphToPlayerChoice
            ) { session.morphToPlayerChoice = it }
            Spacer(Modifier.height(14.dp))
            StudioButton(
                "Apply and close",
                onClick = {
                    session.notify("Avatar set to ${session.avatarKind.title}")
                    onClose()
                },
                primary = true,
                glyph = Glyph.CHECK,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AudioSheet(
    session: StudioSession,
    controller: StudioController,
    onClose: () -> Unit
) {
    val preview = rememberAudioPreview(session.audio)
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<String?>("All") }
    val genres = remember { listOf("All") + ToolboxCatalog.audio.map { it.genre }.distinct().take(8) }

    StudioSheet(title = "Audio Browser", subtitle = "Preview and insert sounds into SoundService", onClose = onClose) {
        Column(modifier = Modifier.padding(horizontal = 10.dp)) {
            AudioPreviewBar(preview, session)
            Spacer(Modifier.height(8.dp))
            SearchField(value = query, onValueChange = { query = it }, placeholder = "Search audio")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (item in genres) {
                    val active = genre == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) LocalStudioPalette.current.accent else LocalStudioPalette.current.input)
                            .clickable { genre = item }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            item,
                            color = if (active) Color.White else LocalStudioPalette.current.dim,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val items = ToolboxCatalog.audio.filter { audio ->
                (genre == null || genre == "All" || audio.genre == genre) &&
                    (query.isBlank() || audio.name.contains(query, true) || audio.creator.contains(query, true))
            }
            if (items.isEmpty()) {
                EmptyState(Glyph.AUDIO, "No results", "Try a different search or genre.")
            } else {
                for (item in items) {
                    AudioBrowserRow(item, preview, session, controller)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AudioBrowserRow(
    item: AudioItem,
    preview: AudioEngine.PreviewState,
    session: StudioSession,
    controller: StudioController
) {
    val palette = LocalStudioPalette.current
    val active = preview.url == item.url
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) palette.select else Color.Transparent)
            .clickable {
                if (active && preview.playing) {
                    session.audio.pausePreview()
                } else if (active) {
                    session.audio.playPreview()
                } else {
                    session.audio.preview(item.url)
                }
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(Glyph.AUDIO, palette.accent, size = 32.dp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                "${item.creator} · ${item.genre} · ${item.displayLength}",
                color = palette.dim,
                fontSize = 9.sp
            )
        }
        StudioButton(
            "Insert",
            { controller.insertAudio(item) },
            compact = true,
            glyph = Glyph.PLUS
        )
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(androidx.compose.foundation.horizontalScroll(androidx.compose.foundation.rememberScrollState()))
