package com.tetrazbloxy.mobilestudio.ui

import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.core.OutputLog
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession

@Composable
fun OutputPanel(
    session: StudioSession,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var lines by remember(session) { mutableStateOf(session.output.snapshot()) }
    var level by remember { mutableStateOf<OutputLog.Level?>(null) }
    var query by remember { mutableStateOf("") }
    var autoScroll by remember { mutableStateOf(true) }
    val main = remember { Handler(Looper.getMainLooper()) }
    val listState = rememberLazyListState()

    DisposableEffect(session.output) {
        val remove = session.output.addListener {
            main.post { lines = session.output.snapshot() }
        }
        onDispose { remove() }
    }

    val visible = remember(lines, level, query) {
        lines.filter { line ->
            (level == null || line.level == level) &&
                (query.isBlank() || line.text.contains(query, true))
        }
    }

    LaunchedEffect(visible.size, autoScroll) {
        if (autoScroll && visible.isNotEmpty()) {
            listState.animateScrollToItem(visible.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(palette.panel)) {
        PanelHeader("Output", Glyph.TERMINAL) {
            StudioIconButton(
                glyph = Glyph.TRASH,
                contentDescription = "Clear output",
                onClick = {
                    session.output.clear()
                    lines = emptyList()
                },
                tint = palette.dim,
                size = 28.dp,
                iconSize = 15.dp
            )
            StudioIconButton(
                glyph = Glyph.ARROW_DOWN,
                contentDescription = "Auto scroll",
                onClick = { autoScroll = !autoScroll },
                tint = if (autoScroll) palette.accent else palette.dim,
                size = 28.dp,
                iconSize = 15.dp
            )
            StudioIconButton(
                glyph = Glyph.CLOSE,
                contentDescription = "Close Output",
                onClick = { session.toggleDock("output") },
                tint = palette.dim,
                size = 28.dp,
                iconSize = 15.dp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutputFilter("All", level == null, palette.accent) { level = null }
            OutputFilter("Info", level == OutputLog.Level.INFO, palette.info) {
                level = OutputLog.Level.INFO
            }
            OutputFilter("Warn", level == OutputLog.Level.WARN, palette.warn) {
                level = OutputLog.Level.WARN
            }
            OutputFilter("Error", level == OutputLog.Level.ERROR, palette.error) {
                level = OutputLog.Level.ERROR
            }
            OutputFilter("Print", level == OutputLog.Level.PRINT, palette.text) {
                level = OutputLog.Level.PRINT
            }
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Filter output",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
        )

        if (visible.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (lines.isEmpty()) "Output is empty." else "No matching log lines.",
                    color = palette.dim,
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(visible) { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            formatClock(line.time),
                            color = palette.dim.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(38.dp)
                        )
                        StudioIcon(
                            when (line.level) {
                                OutputLog.Level.INFO -> Glyph.INFO
                                OutputLog.Level.WARN -> Glyph.WARNING
                                OutputLog.Level.ERROR -> Glyph.WARNING
                                OutputLog.Level.PRINT -> Glyph.TERMINAL
                            },
                            when (line.level) {
                                OutputLog.Level.INFO -> palette.info
                                OutputLog.Level.WARN -> palette.warn
                                OutputLog.Level.ERROR -> palette.error
                                OutputLog.Level.PRINT -> palette.play
                            },
                            size = 11.dp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            line.text,
                            color = when (line.level) {
                                OutputLog.Level.WARN -> palette.warn
                                OutputLog.Level.ERROR -> palette.error
                                OutputLog.Level.PRINT -> palette.text.copy(alpha = 0.85f)
                                OutputLog.Level.INFO -> palette.text
                            },
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun OutputFilter(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) accent.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (active) accent else palette.dim,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
