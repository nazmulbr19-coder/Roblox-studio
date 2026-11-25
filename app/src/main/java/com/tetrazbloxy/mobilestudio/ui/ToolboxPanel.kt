package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.AudioItem
import com.tetrazbloxy.mobilestudio.studio.CatalogItem
import com.tetrazbloxy.mobilestudio.studio.LibraryModel
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import com.tetrazbloxy.mobilestudio.studio.ToolboxCatalog
import com.tetrazbloxy.mobilestudio.studio.ToolboxCategory

@Composable
fun ToolboxPanel(
    session: StudioSession,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var category by remember { mutableStateOf(ToolboxCategory.PARTS) }
    var query by remember { mutableStateOf("") }
    val preview = rememberAudioPreview(session.audio)

    Column(modifier = modifier.fillMaxSize().background(palette.panel)) {
        PanelHeader("Toolbox", Glyph.CHECK) {
            StudioIconButton(
                glyph = Glyph.SAVE,
                contentDescription = "Save selection to My Models",
                onClick = {
                    if (session.selection.isEmpty()) {
                        session.notify("Select something to save")
                    } else {
                        session.dialog = AppDialog.Prompt(
                            title = "Save to My Models",
                            label = "Model name",
                            initial = session.selection.first().name
                        ) { name ->
                            session.dialog = null
                            if (!name.isNullOrBlank()) controller.saveSelectionToLibrary(name)
                        }
                    }
                },
                tint = palette.dim,
                size = 30.dp,
                iconSize = 16.dp
            )
            StudioIconButton(
                glyph = Glyph.CLOSE,
                contentDescription = "Close Toolbox",
                onClick = { session.toggleDock("toolbox") },
                tint = palette.dim,
                size = 30.dp,
                iconSize = 15.dp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (entry in ToolboxCategory.entries) {
                val active = entry == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) palette.accent else palette.input)
                        .clickable { category = entry }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        entry.title,
                        color = if (active) Color.White else palette.dim,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        if (category != ToolboxCategory.PLUGINS && category != ToolboxCategory.INVENTORY) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search ${category.title.lowercase()}",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        when (category) {
            ToolboxCategory.PARTS,
            ToolboxCategory.OBJECTS,
            ToolboxCategory.MODELS -> {
                val items = ToolboxCatalog.search(query, category)
                if (items.isEmpty()) {
                    EmptyState(
                        glyph = Glyph.SEARCH,
                        title = "No ${category.title.lowercase()}",
                        subtitle = "Try another search term."
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items, key = { it.id }) { item ->
                            CatalogRow(item) {
                                controller.insertCatalogItem(item)
                                session.notify("Inserted ${item.name}")
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
            ToolboxCategory.AUDIO -> {
                val audio = ToolboxCatalog.audio.filter {
                    query.isBlank() || it.name.contains(query, true) ||
                        it.creator.contains(query, true) || it.genre.contains(query, true)
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    AudioPreviewBar(preview, session)
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(audio, key = { it.id }) { item ->
                            AudioRow(
                                item = item,
                                previewUrl = preview.url,
                                playing = preview.playing
                            ) {
                                if (preview.url == item.url && preview.playing) {
                                    session.audio.pausePreview()
                                } else if (preview.url == item.url) {
                                    session.audio.playPreview()
                                } else {
                                    session.audio.preview(item.url)
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(modifier = Modifier.padding(start = 12.dp, end = 8.dp, bottom = 6.dp)) {
                                StudioButton(
                                    "Insert",
                                    { controller.insertAudio(item) },
                                    compact = true,
                                    glyph = Glyph.PLUS
                                )
                                Spacer(Modifier.weight(1f))
                                StudioButton(
                                    "Browser",
                                    { session.sheet = AppSheet.AUDIO },
                                    compact = true,
                                    glyph = Glyph.AUDIO
                                )
                            }
                        }
                        item { Spacer(Modifier.height(10.dp)) }
                    }
                }
            }
            ToolboxCategory.LIBRARY -> {
                val models = session.library.list()
                if (models.isEmpty()) {
                    EmptyState(
                        glyph = Glyph.LAYERS,
                        title = "My Models is empty",
                        subtitle = "Select instances and use the save button in the Toolbox header.",
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(models, key = { it.file.absolutePath }) { model ->
                            LibraryRow(model, session, controller)
                        }
                    }
                }
            }
            ToolboxCategory.PLUGINS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        StudioButton(
                            "Manager",
                            { session.sheet = AppSheet.PLUGINS },
                            compact = true,
                            glyph = Glyph.PLUGIN,
                            modifier = Modifier.weight(1f)
                        )
                        StudioButton(
                            "Roblox ID",
                            {
                                session.dialog = AppDialog.Prompt(
                                    title = "Install plugin from Roblox",
                                    label = "Asset ID or link"
                                ) { input ->
                                    session.dialog = null
                                    if (!input.isNullOrBlank()) controller.installPluginFromRoblox(input)
                                }
                            },
                            compact = true,
                            glyph = Glyph.DOWNLOAD,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (session.pluginHost.entries.isEmpty()) {
                        EmptyState(
                            glyph = Glyph.PLUGIN,
                            title = "No plugins installed",
                            subtitle = "Install a .rbxm, .rbxmx or .lua plugin from Roblox or a file.",
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(session.pluginHost.entries, key = { it.file.absolutePath }) { entry ->
                                PluginRow(session, entry)
                            }
                        }
                    }
                }
            }
            ToolboxCategory.INVENTORY -> {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    if (!session.accounts.signedIn) {
                        EmptyState(
                            glyph = Glyph.ACCOUNT,
                            title = "Sign in for inventory",
                            subtitle = "Roblox inventory, owned models and audio need an account.",
                            action = {
                                StudioButton(
                                    "Sign in",
                                    { session.sheet = AppSheet.ACCOUNT },
                                    primary = true,
                                    glyph = Glyph.ACCOUNT
                                )
                            }
                        )
                    } else {
                        Text(
                            "Roblox audio library",
                            color = palette.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Preview tracks and insert a Sound into SoundService.",
                            color = palette.dim,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        for (item in ToolboxCatalog.audio.take(6)) {
                            AudioRow(
                                item = item,
                                previewUrl = preview.url,
                                playing = preview.playing
                            ) {
                                if (preview.url == item.url && preview.playing) {
                                    session.audio.pausePreview()
                                } else if (preview.url == item.url) {
                                    session.audio.playPreview()
                                } else {
                                    session.audio.preview(item.url)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        StudioButton(
                            "Open audio browser",
                            { session.sheet = AppSheet.AUDIO },
                            glyph = Glyph.AUDIO
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(item: CatalogItem, onInsert: () -> Unit) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInsert)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(
            when (item.category) {
                ToolboxCategory.PARTS -> Glyph.CUBE
                ToolboxCategory.MODELS -> Glyph.LAYERS
                else -> Glyph.CHECK
            },
            Color(item.accent),
            size = 34.dp
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                item.description,
                color = palette.dim,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StudioIconButton(
            glyph = Glyph.PLUS,
            contentDescription = "Insert ${item.name}",
            onClick = onInsert,
            tint = palette.accent,
            size = 30.dp,
            iconSize = 16.dp
        )
    }
}

@Composable
private fun LibraryRow(
    model: LibraryModel,
    session: StudioSession,
    controller: StudioController
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(Glyph.LAYERS, palette.accent, size = 34.dp)
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(model.name, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                formatRelativeTime(model.modified),
                color = palette.dim,
                fontSize = 9.sp
            )
        }
        StudioIconButton(
            glyph = Glyph.PLUS,
            contentDescription = "Insert ${model.name}",
            onClick = { controller.insertLibraryModel(model) },
            tint = palette.accent,
            size = 28.dp,
            iconSize = 15.dp
        )
        StudioIconButton(
            glyph = Glyph.TRASH,
            contentDescription = "Delete ${model.name}",
            onClick = {
                if (session.library.delete(model)) session.notify("Deleted ${model.name}")
            },
            tint = palette.dim,
            size = 28.dp,
            iconSize = 15.dp
        )
    }
}

@Composable
private fun PluginRow(
    session: StudioSession,
    entry: com.tetrazbloxy.mobilestudio.plugins.PluginHost.Entry
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(
            if (entry.kind?.extension == "lua") Glyph.CODE else Glyph.PLUGIN,
            if (entry.error != null) palette.error else palette.accent,
            size = 32.dp
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, color = palette.text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text(
                when {
                    entry.error != null -> entry.error
                    entry.kind?.extension == "lua" -> "Lua · metadata only"
                    else -> "${entry.kind?.extension?.uppercase() ?: "?"} · ${entry.scriptCount} script(s)"
                },
                color = if (entry.error != null) palette.error else palette.dim,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = entry.enabled,
            onCheckedChange = { enabled ->
                session.pluginHost.setEnabled(entry, enabled)
                if (enabled) session.pluginHost.loadAll() else session.pluginHost.deactivate()
            },
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
fun AudioPreviewBar(
    preview: com.tetrazbloxy.mobilestudio.audio.AudioEngine.PreviewState,
    session: StudioSession
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIconButton(
            glyph = if (preview.playing) Glyph.STOP else Glyph.PLAY,
            contentDescription = if (preview.playing) "Pause" else "Play preview",
            onClick = {
                if (preview.url == null) {
                    session.notify("Pick a track to preview")
                } else {
                    session.audio.togglePreview()
                }
            },
            tint = palette.accent,
            size = 30.dp,
            iconSize = 16.dp
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                when {
                    preview.loading -> "Loading preview…"
                    preview.error != null -> preview.error
                    preview.url == null -> "No preview selected"
                    preview.playing -> "Playing preview"
                    else -> "Preview paused"
                },
                color = if (preview.error != null) palette.error else palette.text,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${preview.positionMs / 1000}s / ${(preview.durationMs / 1000).coerceAtLeast(0)}s",
                color = palette.dim,
                fontSize = 8.sp
            )
        }
        StudioIconButton(
            glyph = Glyph.CLOSE,
            contentDescription = "Stop preview",
            onClick = { session.audio.stopPreview() },
            tint = palette.dim,
            size = 28.dp,
            iconSize = 14.dp
        )
    }
}

@Composable
private fun AudioRow(
    item: AudioItem,
    previewUrl: String?,
    playing: Boolean,
    onPreview: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val active = previewUrl == item.url
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) palette.select else Color.Transparent)
            .clickable(onClick = onPreview)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(palette.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            StudioIcon(
                if (active && playing) Glyph.STOP else Glyph.PLAY,
                palette.accent,
                size = 15.dp
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                color = palette.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.creator} · ${item.genre} · ${item.displayLength}",
                color = palette.dim,
                fontSize = 8.sp,
                maxLines = 1
            )
        }
        StatusPill(item.displayLength, palette.info)
    }
}
