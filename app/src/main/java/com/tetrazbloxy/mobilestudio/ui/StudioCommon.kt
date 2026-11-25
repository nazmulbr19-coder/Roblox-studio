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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tetrazbloxy.mobilestudio.audio.AudioEngine
import com.tetrazbloxy.mobilestudio.model.ClassDb
import com.tetrazbloxy.mobilestudio.studio.StudioSession

/** Shared modal sheet used by the file menu, settings, account and plugins. */
@Composable
fun StudioSheet(
    title: String,
    subtitle: String? = null,
    onClose: () -> Unit,
    maxWidth: Dp = 560.dp,
    maxHeight: Dp = 720.dp,
    content: @Composable () -> Unit
) {
    val palette = LocalStudioPalette.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(onClick = onClose)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
                    .clickable { },
                color = palette.card,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                color = palette.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    subtitle,
                                    color = palette.dim,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        StudioIconButton(
                            glyph = Glyph.CLOSE,
                            contentDescription = "Close",
                            onClick = onClose,
                            tint = palette.dim,
                            size = 34.dp,
                            iconSize = 17.dp
                        )
                    }
                    StudioDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 10.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun AccountChip(session: StudioSession, onClick: () -> Unit) {
    val palette = LocalStudioPalette.current
    val accounts = session.accounts
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (accounts.signedIn) palette.accent else palette.hover),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (accounts.signedIn) {
                    (accounts.displayName.ifBlank { accounts.userName }).take(1).uppercase()
                } else {
                    "?"
                },
                color = if (accounts.signedIn) Color.White else palette.dim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = if (accounts.signedIn) {
                "@" + accounts.userName.ifBlank { "signed in" }
            } else {
                "Sign in"
            },
            color = palette.text,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlyphBadge(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        StudioIcon(glyph, tint, size = size * 0.52f)
    }
}

/** Original icon mapping for the instance tree and property rows. */
fun classGlyph(className: String): Glyph = when {
    className == "Workspace" -> Glyph.WORLD
    className == "Model" -> Glyph.LAYERS
    className == "Folder" -> Glyph.FOLDER
    className == "SpawnLocation" -> Glyph.SPAWN
    className == "Humanoid" || className == "Player" -> Glyph.ACCOUNT
    className == "Lighting" -> Glyph.LIGHT
    className == "Sound" || className == "SoundService" -> Glyph.AUDIO
    className == "Script" || className == "LocalScript" || className == "ModuleScript" -> Glyph.SCRIPT
    className == "WeldConstraint" || className == "Constraint" -> Glyph.LINK
    className == "WedgePart" -> Glyph.SHAPES
    className == "MeshPart" -> Glyph.CUBE
    ClassDb.isA(className, "BasePart") -> Glyph.CUBE
    ClassDb.isA(className, "GuiObject") || className == "ScreenGui" ||
        className == "BillboardGui" || className == "SurfaceGui" -> Glyph.GUI
    className == "DataModel" -> Glyph.GRID
    else -> Glyph.CUBE
}

fun classAccent(className: String): Color = when {
    className == "Script" || className == "LocalScript" || className == "ModuleScript" ->
        Color(0xFF60A5FA)
    ClassDb.isA(className, "BasePart") -> Color(0xFFF59E0B)
    className == "Model" || className == "Folder" -> Color(0xFFA78BFA)
    className == "Workspace" -> Color(0xFF34D399)
    className == "SpawnLocation" -> Color(0xFF4ADE80)
    ClassDb.isA(className, "GuiObject") || className == "ScreenGui" ||
        className == "BillboardGui" || className == "SurfaceGui" -> Color(0xFF22D3EE)
    className == "Humanoid" -> Color(0xFFFBBF24)
    className == "Lighting" -> Color(0xFFFDE68A)
    else -> Color(0xFF94A3B8)
}

/** Observes [AudioEngine] preview state from the main thread. */
@Composable
fun rememberAudioPreview(audio: AudioEngine): AudioEngine.PreviewState {
    var state by remember { mutableStateOf(audio.currentPreviewState()) }
    val main = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(audio) {
        val remove = audio.addPreviewListener { next ->
            main.post { state = next }
        }
        onDispose { remove() }
    }
    return state
}

@Composable
fun KeyValueLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = palette.dim,
            fontSize = 11.sp,
            modifier = Modifier.widthIn(min = 92.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color = palette.text,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        content()
    }
}

@Composable
fun ThinDivider(inset: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(1.dp)
            .background(LocalStudioPalette.current.border.copy(alpha = 0.7f))
    )
}
