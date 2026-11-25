package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.model.Vector3
import com.tetrazbloxy.mobilestudio.playtest.PlaytestSession
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import com.tetrazbloxy.mobilestudio.ui.render.SceneCamera
import com.tetrazbloxy.mobilestudio.ui.render.SoftwareScene
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Viewport(
    session: StudioSession,
    controller: StudioController,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val play = session.play
    val playing = session.playing && play != null

    val parts = remember(session.document, session.sceneVersion) {
        SoftwareScene.partsOf(session.document)
    }
    val selected = session.selection.filterIsInstance<com.tetrazbloxy.mobilestudio.model.BasePart>().toSet()

    val camera = if (playing && play != null) {
        playCamera(play)
    } else {
        SoftwareScene.cameraForOrbit(
            session.camTarget,
            session.camYaw,
            session.camPitch,
            session.camDistance
        )
    }

    Box(
        modifier = modifier
            .background(palette.bg)
            .onSizeChanged { size = it }
            .pointerInput(session.playing) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (session.playing) {
                        session.play?.look(-pan.x * 0.006f, -pan.y * 0.006f)
                        if (zoom != 1f) session.play?.zoom((1f - zoom) * 14f)
                    } else {
                        session.camYaw -= pan.x * 0.008f
                        session.camPitch = (session.camPitch - pan.y * 0.008f)
                            .coerceIn(-1.36f, 1.36f)
                        if (zoom != 1f) {
                            session.camDistance = (session.camDistance / zoom)
                                .coerceIn(2.5f, 400f)
                        }
                        session.touchScene()
                    }
                }
            }
            .pointerInput(
                session.playing,
                session.sceneVersion,
                session.camYaw,
                session.camPitch,
                session.camTarget,
                session.camDistance,
                size
            ) {
                detectTapGestures(
                    onTap = { offset ->
                        if (session.playing) return@detectTapGestures
                        if (size.width <= 0) return@detectTapGestures
                        val hit = SoftwareScene.pickPart(
                            camera,
                            parts,
                            size.width.toFloat(),
                            size.height.toFloat(),
                            offset.x,
                            offset.y
                        )
                        if (hit != null) {
                            session.setSelection(listOf(hit))
                        } else {
                            controller.clearSelection()
                        }
                    },
                    onDoubleTap = {
                        if (!session.playing) controller.focusSelection()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Reading the scene version makes document mutations repaint.
            session.sceneVersion
            SoftwareScene.draw(
                drawScope = this,
                camera = camera,
                parts = parts,
                selected = selected,
                accent = palette.accent,
                background = palette.bg,
                showGrid = session.showGrid,
                characterPosition = if (playing) play?.position else null,
                characterYaw = play?.facingYaw ?: 0f,
                wireframe = session.showWireframe
            )
        }

        if (!playing) {
            EditorViewportOverlay(session, controller, parts.size)
        } else if (play != null) {
            PlayHud(play, session, controller)
        }
    }
}

private fun playCamera(play: PlaytestSession): SceneCamera {
    val forward = Vector3(
        -sin(play.cameraYaw) * cos(play.cameraPitch),
        sin(play.cameraPitch),
        -cos(play.cameraYaw) * cos(play.cameraPitch)
    )
    val target = play.cameraTarget + Vector3(0f, 0.6f, 0f)
    return SceneCamera(target - forward * play.cameraDistance, target)
}

@Composable
private fun EditorViewportOverlay(
    session: StudioSession,
    controller: StudioController,
    partCount: Int
) {
    val palette = LocalStudioPalette.current
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = palette.card.copy(alpha = 0.88f),
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioIcon(Glyph.CUBE, palette.accent, size = 15.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Perspective", color = palette.text, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = palette.card.copy(alpha = 0.88f),
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Text(
                    "$partCount parts",
                    color = palette.dim,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ViewportButton(Glyph.ZOOM_IN, "Zoom in") {
                session.camDistance = (session.camDistance * 0.82f).coerceAtLeast(2.5f)
                session.touchScene()
            }
            ViewportButton(Glyph.ZOOM_OUT, "Zoom out") {
                session.camDistance = (session.camDistance * 1.22f).coerceAtMost(400f)
                session.touchScene()
            }
            ViewportButton(Glyph.FRAME, "Focus selection") { controller.focusSelection() }
            ViewportButton(
                Glyph.GRID,
                "Toggle grid",
                tint = if (session.showGrid) palette.accent else palette.dim
            ) {
                session.showGrid = !session.showGrid
                session.touchScene()
            }
        }

        if (session.selection.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart),
                color = palette.card.copy(alpha = 0.94f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioIcon(
                        if (session.selectedPart != null) Glyph.CUBE else Glyph.LAYERS,
                        palette.accent,
                        size = 15.dp
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        session.selectionLabel,
                        color = palette.text,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    StudioIconButton(
                        glyph = Glyph.CLOSE,
                        contentDescription = "Clear selection",
                        onClick = { controller.clearSelection() },
                        tint = palette.dim,
                        size = 24.dp,
                        iconSize = 13.dp
                    )
                }
            }
        }

        if (partCount == 0) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StudioIcon(Glyph.CUBE, palette.dim.copy(alpha = 0.7f), size = 44.dp)
                Spacer(Modifier.height(10.dp))
                Text("This place is empty", color = palette.text, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Insert a part from the ribbon or toolbox",
                    color = palette.dim,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            "Drag to orbit · Pinch to zoom · Double-tap to focus",
            color = palette.dim.copy(alpha = 0.7f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun ViewportButton(
    glyph: Glyph,
    description: String,
    tint: Color = LocalStudioPalette.current.text,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    androidx.compose.material3.Surface(
        color = palette.card.copy(alpha = 0.9f),
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        StudioIconButton(
            glyph = glyph,
            contentDescription = description,
            onClick = onClick,
            tint = tint,
            size = 32.dp,
            iconSize = 17.dp
        )
    }
}
