package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.playtest.AvatarKind
import com.tetrazbloxy.mobilestudio.playtest.ChatLine
import com.tetrazbloxy.mobilestudio.playtest.HudPage
import com.tetrazbloxy.mobilestudio.playtest.PlaytestSession
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import kotlin.math.sqrt

@Composable
fun PlayHud(play: PlaytestSession, session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar ---------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudPill(onClick = { play.showPage(HudPage.MENU) }) {
                StudioIcon(Glyph.MENU, palette.text, size = 17.dp)
            }
            Spacer(Modifier.width(8.dp))
            HudPill(onClick = { play.showPage(HudPage.PEOPLE) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudioIcon(Glyph.PEOPLE, palette.accent, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${play.people.size}",
                        color = palette.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudPill(onClick = { play.showPage(HudPage.SETTINGS) }, icon = Glyph.SETTINGS)
            Spacer(Modifier.width(8.dp))
            HudPill(onClick = { play.showPage(HudPage.HELP) }, icon = Glyph.HELP)
            if (session.settings.showFps) {
                Spacer(Modifier.width(8.dp))
                HudPill(onClick = {}) {
                    Text(
                        "${play.fps} FPS",
                        color = if (play.fps < 30) palette.warn else palette.play,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Chat log --------------------------------------------------------
        if (!play.chatOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 142.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val visible = play.chat.takeLast(5)
                for (line in visible) {
                    ChatBubble(line)
                }
            }
        }

        // Mobile controls -------------------------------------------------
        if (!play.chatOpen) {
            Joystick(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 26.dp),
                onMove = { forward, strafe -> play.move(forward, strafe) },
                onRelease = { play.releaseMove() }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleControl(
                        glyph = Glyph.BOLT,
                        tint = if (play.sprinting) palette.accent else palette.text,
                        size = 46
                    ) { play.setSprinting(!play.sprinting) }
                    Spacer(Modifier.width(10.dp))
                    CircleControl(
                        glyph = Glyph.CHAT,
                        tint = palette.text,
                        size = 46
                    ) { play.openChat() }
                }
                Spacer(Modifier.height(12.dp))
                CircleControl(
                    glyph = Glyph.ARROW_UP,
                    tint = Color.White,
                    background = palette.accent,
                    size = 74
                ) { play.jump() }
            }
        }

        // Chat composer ---------------------------------------------------
        if (play.chatOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Surface(
                    color = palette.card.copy(alpha = 0.97f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Chat",
                            color = palette.text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StudioField(
                                value = play.chatDraft,
                                onValueChange = { play.updateDraft(it) },
                                placeholder = "Say something…",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            StudioButton("Send", onClick = {
                                play.sendChat(play.chatDraft)
                                play.closeChat()
                            }, primary = true, compact = true)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("/help", "/reset", "/clear").forEach { command ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(palette.input)
                                        .clickable {
                                            play.sendChat(command)
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(command, color = palette.dim, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pages -----------------------------------------------------------
        when (play.hudPage) {
            HudPage.NONE -> Unit
            HudPage.MENU -> PageScrim({ play.hidePage() }) {
                PlayPage("Paused", "Playing ${session.documentName}") {
                    PlayMenuButton(Glyph.PLAY, "Resume", "Back to the playtest") {
                        play.hidePage()
                    }
                    PlayMenuButton(Glyph.REFRESH, "Reset character", "Return to the spawn point") {
                        play.resetCharacter()
                        play.hidePage()
                    }
                    PlayMenuButton(Glyph.PEOPLE, "People", "Everyone in this server") {
                        play.showPage(HudPage.PEOPLE)
                    }
                    PlayMenuButton(Glyph.SETTINGS, "Settings", "Camera, movement and HUD") {
                        play.showPage(HudPage.SETTINGS)
                    }
                    PlayMenuButton(Glyph.HELP, "Help", "Controls and commands") {
                        play.showPage(HudPage.HELP)
                    }
                    PlayMenuButton(Glyph.LOGOUT, "Leave playtest", "Return to the editor", danger = true) {
                        play.showPage(HudPage.CONFIRM)
                    }
                }
            }
            HudPage.PEOPLE -> PageScrim({ play.hidePage() }) {
                PlayPage("People", "This server") {
                    for (person in play.people) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (person.isYou) palette.accent.copy(alpha = 0.25f)
                                        else palette.input
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    person.name.take(1).uppercase(),
                                    color = if (person.isYou) palette.accent else palette.dim,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(person.name, color = palette.text, fontSize = 13.sp)
                                Text(
                                    if (person.isYou) "You · ${person.health.toInt()} HP" else "Health ${person.health.toInt()}",
                                    color = palette.dim,
                                    fontSize = 10.sp
                                )
                            }
                            if (person.isYou) TagChip("YOU", palette.accent)
                        }
                    }
                }
            }
            HudPage.SETTINGS -> PageScrim({ play.hidePage() }) {
                PlayPage("Playtest settings", "These do not change the saved place") {
                    SliderRow(
                        "Walk speed",
                        play.walkSpeed,
                        { play.setWalkSpeed(it) },
                        1f..80f,
                        valueLabel = { "${it.toInt()}" }
                    )
                    SliderRow(
                        "Jump power",
                        play.jumpPower,
                        { play.setJumpPower(it) },
                        10f..120f,
                        valueLabel = { "${it.toInt()}" }
                    )
                    SliderRow(
                        "Third-person distance",
                        play.cameraDistance,
                        { play.setCameraDistance(it) },
                        4f..42f,
                        valueLabel = { "${it.toInt()}" }
                    )
                    SliderRow(
                        "Play camera sensitivity",
                        session.settings.playCameraSensitivity,
                        { session.settings.playCameraSensitivity = it },
                        0.2f..3f
                    )
                    SliderRow(
                        "Master volume",
                        session.settings.volume.toFloat(),
                        { session.settings.volume = it.toInt().coerceIn(0, 10) },
                        0f..10f,
                        valueLabel = { "${it.toInt()}" }
                    )
                    ToggleRow(
                        "Show FPS",
                        "Display the live frame rate",
                        session.settings.showFps
                    ) { session.settings.showFps = it }
                    ToggleRow(
                        "Hide windows in playtest",
                        "Hide the docks when a playtest starts",
                        session.settings.hideWindowsInPlaytest
                    ) { session.settings.hideWindowsInPlaytest = it }
                }
            }
            HudPage.HELP -> PageScrim({ play.hidePage() }) {
                PlayPage("Help & controls", "Mobile Studio 2.5") {
                    HelpLine("Drag the viewport", "Look around")
                    HelpLine("Left stick", "Move your character")
                    HelpLine("Jump button", "Jump")
                    HelpLine("Bolt button", "Toggle sprint")
                    HelpLine("Chat button", "Open chat and commands")
                    HelpLine("/reset", "Respawn at the spawn point")
                    HelpLine("/zoom 20", "Set camera distance")
                    HelpLine("/speed 24", "Set walk speed")
                    HelpLine("/clear", "Clear the chat log")
                    Spacer(Modifier.height(8.dp))
                    PlayMenuButton(Glyph.ARROW_LEFT, "Back", "") { play.hidePage() }
                }
            }
            HudPage.CONFIRM -> PageScrim({ play.showPage(HudPage.MENU) }) {
                PlayPage("Leave playtest?", "Unsaved playtest progress is discarded.") {
                    PlayMenuButton(Glyph.LOGOUT, "Leave playtest", "Return to the editor", danger = true) {
                        play.hidePage()
                        controller.stopPlay()
                    }
                    PlayMenuButton(Glyph.ARROW_LEFT, "Keep playing", "") {
                        play.showPage(HudPage.MENU)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPill(
    onClick: () -> Unit,
    icon: Glyph? = null,
    content: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.card.copy(alpha = 0.9f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) StudioIcon(icon, palette.text, size = 17.dp)
            content?.invoke(this)
        }
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    val palette = LocalStudioPalette.current
    if (line.system) {
        Text(
            line.text,
            color = palette.dim,
            fontSize = 10.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    } else {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Text(
                "${line.author}: ",
                color = if (line.author == "You") palette.accent else Color(0xFF7DD3FC),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(line.text, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Joystick(
    modifier: Modifier = Modifier,
    onMove: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    val palette = LocalStudioPalette.current
    val density = LocalDensity.current
    var knob by remember { mutableStateOf(Offset.Zero) }
    val limit = with(density) { 38.dp.toPx() }

    Box(
        modifier = modifier
            .size(126.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { knob = Offset.Zero },
                    onDragEnd = {
                        knob = Offset.Zero
                        onRelease()
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        onRelease()
                    }
                ) { change, drag ->
                    change.consume()
                    val next = knob + drag
                    val length = sqrt(next.x * next.x + next.y * next.y)
                    knob = if (length > limit) next * (limit / length) else next
                    onMove(-knob.y / limit, knob.x / limit)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.Black.copy(alpha = 0.28f), radius = size.minDimension * 0.5f)
            drawCircle(
                palette.text.copy(alpha = 0.25f),
                radius = size.minDimension * 0.5f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawCircle(
                palette.text.copy(alpha = 0.12f),
                radius = size.minDimension * 0.3f
            )
            drawCircle(
                palette.accent,
                radius = size.minDimension * 0.19f,
                center = center + knob
            )
        }
    }
}

@Composable
private fun CircleControl(
    glyph: Glyph,
    tint: Color,
    size: Int,
    background: Color = LocalStudioPalette.current.card.copy(alpha = 0.9f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        StudioIcon(glyph, tint, size = (size * 0.4f).dp)
    }
}

@Composable
private fun PageScrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.clickable(onClick = {})) {
            content()
        }
    }
}

@Composable
private fun PlayPage(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val palette = LocalStudioPalette.current
    Surface(
        color = palette.card,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 460.dp)
                .heightIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(title, color = palette.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = palette.dim, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PlayMenuButton(
    glyph: Glyph,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (danger) palette.stop.copy(alpha = 0.14f) else palette.input
                ),
            contentAlignment = Alignment.Center
        ) {
            StudioIcon(glyph, if (danger) palette.stop else palette.accent, size = 19.dp)
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) palette.stop else palette.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = palette.dim, fontSize = 10.sp)
            }
        }
        StudioIcon(Glyph.CHEVRON_RIGHT, palette.dim, size = 15.dp)
    }
}

@Composable
private fun HelpLine(action: String, description: String) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.input)
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(action, color = palette.accent, fontSize = 10.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(description, color = palette.dim, fontSize = 11.sp)
    }
}

/** Avatar chooser shared by the avatar sheet and playtest menu. */
@Composable
fun AvatarKindPicker(
    selected: AvatarKind,
    onSelect: (AvatarKind) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (kind in AvatarKind.entries) {
            val active = kind == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) palette.select else palette.input)
                    .clickable { onSelect(kind) }
                    .padding(11.dp)
            ) {
                StudioIcon(
                    if (kind == AvatarKind.R6) Glyph.ACCOUNT else Glyph.PEOPLE,
                    if (active) palette.accent else palette.dim,
                    size = 22.dp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    kind.title,
                    color = if (active) palette.text else palette.dim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    kind.description,
                    color = palette.dim,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
