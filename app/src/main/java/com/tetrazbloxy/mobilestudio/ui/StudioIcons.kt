package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Original 24×24 glyph vocabulary used across the app. */
enum class Glyph {
    HOME, CLOCK, GRID, PEOPLE, CUBE, SCRIPT, FOLDER, SPAWN, PLAY, STOP,
    UNDO, REDO, SAVE, FILE, SETTINGS, ACCOUNT, CLOSE, PLUS, MINUS, SEARCH,
    CHEVRON_DOWN, CHEVRON_RIGHT, CHEVRON_LEFT, CHEVRON_UP, PALETTE, BOLT,
    LINK, LIGHT, TEXT, IMAGE, FRAME, GUI, CHAT, MENU, HELP, VOLUME, TRASH,
    EDIT, COPY, PASTE, CUT, ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
    CHECK, WARNING, INFO, PLUGIN, AUDIO, CLOUD, DOWNLOAD, UPLOAD, REFRESH,
    ZOOM_IN, ZOOM_OUT, ROTATE, MOVE, SCALE, SNAP, CODE, TERMINAL, EYE, LOCK,
    WELD, WORLD, CAMERA, SHAPES, LAYERS, DICE, STAR, FLAG, USER_PLUS, LOGOUT,
    FILTER, MORE, DRAG
}

@Composable
fun StudioIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        drawGlyph(glyph, tint)
    }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val unit = size.minDimension / 24f
    if (unit <= 0f) return
    val ox = (size.width - 24f * unit) / 2f
    val oy = (size.height - 24f * unit) / 2f
    val strokeWidth = 1.7f * unit
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val thin = Stroke(width = 1.25f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun p(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = strokeWidth) {
        drawLine(color, p(x1, y1), p(x2, y2), strokeWidth = width, cap = StrokeCap.Round)
    }
    fun box(x: Float, y: Float, w: Float, h: Float, radius: Float = 1.4f, filled: Boolean = false) {
        if (filled) {
            drawRoundRect(
                color, p(x, y), Size(w * unit, h * unit),
                CornerRadius(radius * unit, radius * unit), style = Fill
            )
        } else {
            drawRoundRect(
                color, p(x, y), Size(w * unit, h * unit),
                CornerRadius(radius * unit, radius * unit), style = thin
            )
        }
    }
    fun circle(x: Float, y: Float, r: Float, filled: Boolean = false, width: Float = strokeWidth) {
        drawCircle(
            color, r * unit, p(x, y),
            style = if (filled) Fill else Stroke(width = width)
        )
    }
    fun path(points: List<Pair<Float, Float>>, closed: Boolean = false, filled: Boolean = false) {
        val path = Path()
        if (points.isEmpty()) return
        path.moveTo(p(points[0].first, points[0].second).x, p(points[0].first, points[0].second).y)
        for (index in 1 until points.size) {
            val point = p(points[index].first, points[index].second)
            path.lineTo(point.x, point.y)
        }
        if (closed) path.close()
        drawPath(path, color, style = if (filled) Fill else stroke)
    }
    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float, width: Float = strokeWidth) {
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = p(cx - r, cy - r),
            size = Size(r * 2 * unit, r * 2 * unit),
            style = Stroke(width = width, cap = StrokeCap.Round)
        )
    }

    when (glyph) {
        Glyph.HOME -> {
            path(listOf(3f to 11f, 12f to 3.5f, 21f to 11f))
            path(listOf(5f to 10f, 5f to 21f, 19f to 21f, 19f to 10f))
            box(10f, 14f, 4f, 7f, radius = 0.6f)
        }
        Glyph.CLOCK -> {
            circle(12f, 12f, 8.6f)
            line(12f, 7f, 12f, 12.4f)
            line(12f, 12.4f, 15.6f, 14.6f)
        }
        Glyph.GRID -> {
            box(3.5f, 3.5f, 7.4f, 7.4f, 1.6f)
            box(13.1f, 3.5f, 7.4f, 7.4f, 1.6f)
            box(3.5f, 13.1f, 7.4f, 7.4f, 1.6f)
            box(13.1f, 13.1f, 7.4f, 7.4f, 1.6f)
        }
        Glyph.PEOPLE -> {
            circle(9f, 9f, 3.2f)
            arc(9f, 17f, 5.4f, 200f, 140f)
            circle(17f, 10f, 2.4f, width = 1.4f * unit)
            arc(17f, 17.5f, 4.1f, 210f, 120f, width = 1.4f * unit)
        }
        Glyph.CUBE, Glyph.SHAPES -> {
            path(listOf(4f to 8f, 12f to 4f, 20f to 8f, 20f to 17f, 12f to 21f, 4f to 17f), closed = true)
            line(4f, 8f, 12f, 12f)
            line(20f, 8f, 12f, 12f)
            line(12f, 12f, 12f, 21f)
        }
        Glyph.SCRIPT, Glyph.FILE -> {
            path(listOf(6f to 3f, 14.5f to 3f, 19f to 7.5f, 19f to 21f, 6f to 21f), closed = true)
            line(14.5f, 3f, 14.5f, 7.5f)
            line(14.5f, 7.5f, 19f, 7.5f)
            if (glyph == Glyph.SCRIPT) {
                line(9f, 11f, 16f, 11f)
                line(9f, 14f, 16f, 14f)
                line(9f, 17f, 13.5f, 17f)
            }
        }
        Glyph.FOLDER -> {
            path(
                listOf(
                    3.5f to 7f, 10f to 7f, 12f to 9.5f, 20.5f to 9.5f,
                    20.5f to 19.5f, 3.5f to 19.5f
                ),
                closed = true
            )
            line(3.5f, 7f, 3.5f, 6f)
        }
        Glyph.SPAWN -> {
            line(7f, 3.5f, 7f, 21f)
            path(listOf(7f to 4f, 18f to 7.5f, 7f to 11f), closed = true, filled = true)
            line(4f, 21f, 10f, 21f)
        }
        Glyph.PLAY -> path(listOf(8f to 5f, 19.5f to 12f, 8f to 19f), closed = true, filled = true)
        Glyph.STOP -> box(6f, 6f, 12f, 12f, radius = 2f, filled = true)
        Glyph.UNDO -> {
            arc(12f, 12.5f, 7.2f, 30f, 240f)
            path(listOf(4.6f to 8.2f, 4.2f to 13.4f, 9.3f to 12.4f), closed = false)
        }
        Glyph.REDO -> {
            arc(12f, 12.5f, 7.2f, 270f, 240f)
            path(listOf(19.4f to 8.2f, 19.8f to 13.4f, 14.7f to 12.4f), closed = false)
        }
        Glyph.SAVE -> {
            box(4f, 4f, 16f, 16f, 2f)
            box(8f, 4f, 8f, 5.5f, 0.8f)
            box(7.5f, 13f, 9f, 7f, 0.8f)
        }
        Glyph.SETTINGS -> {
            circle(12f, 12f, 3.2f)
            for (index in 0 until 8) {
                val angle = (Math.PI / 4 * index).toFloat()
                val cx = kotlin.math.cos(angle)
                val cy = kotlin.math.sin(angle)
                line(12f + cx * 5.6f, 12f + cy * 5.6f, 12f + cx * 8.6f, 12f + cy * 8.6f)
            }
        }
        Glyph.ACCOUNT -> {
            circle(12f, 8.5f, 3.6f)
            arc(12f, 19.5f, 6.4f, 200f, 140f)
        }
        Glyph.CLOSE -> {
            line(6f, 6f, 18f, 18f)
            line(18f, 6f, 6f, 18f)
        }
        Glyph.PLUS -> {
            line(12f, 5f, 12f, 19f)
            line(5f, 12f, 19f, 12f)
        }
        Glyph.MINUS -> line(5f, 12f, 19f, 12f)
        Glyph.SEARCH -> {
            circle(10.5f, 10.5f, 6f)
            line(15f, 15f, 20f, 20f, width = 2f * unit)
        }
        Glyph.CHEVRON_DOWN -> path(listOf(6f to 9f, 12f to 15f, 18f to 9f))
        Glyph.CHEVRON_RIGHT -> path(listOf(9f to 6f, 15f to 12f, 9f to 18f))
        Glyph.CHEVRON_LEFT -> path(listOf(15f to 6f, 9f to 12f, 15f to 18f))
        Glyph.CHEVRON_UP -> path(listOf(6f to 15f, 12f to 9f, 18f to 15f))
        Glyph.PALETTE -> {
            circle(12f, 12f, 8.5f)
            circle(8f, 9f, 1.3f, filled = true)
            circle(12f, 7f, 1.3f, filled = true)
            circle(16f, 9.5f, 1.3f, filled = true)
            circle(9f, 15f, 1.3f, filled = true)
        }
        Glyph.BOLT -> path(listOf(13.5f to 2.5f, 5.5f to 13f, 11f to 13f, 10f to 21.5f, 18.5f to 10f, 13f to 10f), closed = true, filled = true)
        Glyph.LINK -> {
            arc(9f, 12f, 4.6f, 40f, 220f)
            arc(15f, 12f, 4.6f, 220f, 220f)
            line(9.4f, 9.6f, 14.6f, 14.4f)
        }
        Glyph.LIGHT -> {
            circle(12f, 10f, 5.2f)
            line(9f, 16f, 15f, 16f)
            line(10f, 18.5f, 14f, 18.5f)
            line(11.4f, 20.5f, 12.6f, 20.5f)
        }
        Glyph.TEXT -> {
            line(5f, 5.5f, 19f, 5.5f)
            line(12f, 5.5f, 12f, 19f)
            line(8.5f, 19f, 15.5f, 19f)
        }
        Glyph.IMAGE -> {
            box(3.5f, 5f, 17f, 14f, 1.8f)
            path(listOf(5f to 17f, 9f to 12f, 12f to 15f, 15f to 10f, 19f to 17f))
            circle(16f, 8.5f, 1.5f, filled = true)
        }
        Glyph.FRAME -> {
            box(3.5f, 4.5f, 17f, 15f, 1.6f)
            line(3.5f, 9f, 20.5f, 9f)
        }
        Glyph.GUI -> {
            box(3f, 4.5f, 18f, 15f, 1.8f)
            box(6f, 8f, 7f, 5f, 1f)
            line(6f, 16f, 18f, 16f)
        }
        Glyph.CHAT -> {
            path(
                listOf(
                    3.5f to 6f, 20.5f to 6f, 20.5f to 16f,
                    11f to 16f, 7f to 20f, 7f to 16f, 3.5f to 16f
                ),
                closed = true
            )
            circle(8f, 11f, 0.9f, filled = true)
            circle(12f, 11f, 0.9f, filled = true)
            circle(16f, 11f, 0.9f, filled = true)
        }
        Glyph.MENU -> {
            line(5f, 7f, 19f, 7f)
            line(5f, 12f, 19f, 12f)
            line(5f, 17f, 19f, 17f)
        }
        Glyph.HELP -> {
            circle(12f, 12f, 8.6f)
            arc(12f, 10.5f, 3f, 190f, 220f)
            line(12f, 13.5f, 12f, 15f)
            circle(12f, 17.4f, 1f, filled = true)
        }
        Glyph.VOLUME -> {
            path(listOf(4f to 10f, 8f to 10f, 12f to 6f, 12f to 18f, 8f to 14f, 4f to 14f), closed = true, filled = true)
            arc(12f, 12f, 4f, -55f, 110f)
            arc(12f, 12f, 7f, -55f, 110f)
        }
        Glyph.TRASH -> {
            line(5f, 7f, 19f, 7f)
            line(9f, 7f, 9.5f, 4.5f)
            line(15f, 7f, 14.5f, 4.5f)
            path(listOf(6.5f to 7f, 7.5f to 20f, 16.5f to 20f, 17.5f to 7f))
            line(10f, 10f, 10f, 17f)
            line(14f, 10f, 14f, 17f)
        }
        Glyph.EDIT -> {
            path(listOf(4f to 20f, 5.5f to 14.5f, 16f to 4f, 20f to 8f, 9.5f to 18.5f), closed = true)
            line(14.5f, 5.5f, 18.5f, 9.5f)
        }
        Glyph.COPY -> {
            box(4f, 4f, 11f, 13f, 1.4f)
            box(9f, 8f, 11f, 13f, 1.4f)
        }
        Glyph.PASTE -> {
            box(5f, 5f, 14f, 16f, 1.6f)
            box(9f, 3f, 6f, 3.5f, 1f)
            line(8f, 12f, 16f, 12f)
            line(8f, 16f, 14f, 16f)
        }
        Glyph.CUT -> {
            circle(6.5f, 7f, 2.4f)
            circle(6.5f, 17f, 2.4f)
            line(8.3f, 8.5f, 20f, 18f)
            line(8.3f, 15.5f, 20f, 6f)
        }
        Glyph.ARROW_UP -> {
            line(12f, 20f, 12f, 5f)
            path(listOf(6.5f to 10.5f, 12f to 4.5f, 17.5f to 10.5f))
        }
        Glyph.ARROW_DOWN -> {
            line(12f, 4f, 12f, 19f)
            path(listOf(6.5f to 13.5f, 12f to 19.5f, 17.5f to 13.5f))
        }
        Glyph.ARROW_LEFT -> {
            line(20f, 12f, 5f, 12f)
            path(listOf(10.5f to 6.5f, 4.5f to 12f, 10.5f to 17.5f))
        }
        Glyph.ARROW_RIGHT -> {
            line(4f, 12f, 19f, 12f)
            path(listOf(13.5f to 6.5f, 19.5f to 12f, 13.5f to 17.5f))
        }
        Glyph.CHECK -> path(listOf(4.5f to 12f, 10f to 17.5f, 19.5f to 6.5f))
        Glyph.WARNING -> {
            path(listOf(12f to 3f, 21.5f to 20f, 2.5f to 20f), closed = true)
            line(12f, 9f, 12f, 14.5f)
            circle(12f, 17.2f, 0.9f, filled = true)
        }
        Glyph.INFO -> {
            circle(12f, 12f, 8.6f)
            circle(12f, 7.8f, 1f, filled = true)
            line(12f, 11f, 12f, 17f)
        }
        Glyph.PLUGIN -> {
            path(listOf(8f to 3f, 8f to 8f, 5f to 11f, 5f to 16f, 19f to 16f, 19f to 11f, 16f to 8f, 16f to 3f))
            line(6f, 19f, 18f, 19f)
            line(9f, 16f, 9f, 12f)
            line(15f, 16f, 15f, 12f)
        }
        Glyph.AUDIO -> {
            line(9f, 18f, 9f, 5f)
            line(9f, 5f, 19f, 3.5f)
            line(19f, 3.5f, 19f, 15.5f)
            circle(6.5f, 18f, 2.5f, filled = true)
            circle(16.5f, 15.5f, 2.5f, filled = true)
        }
        Glyph.CLOUD -> {
            circle(8.5f, 13f, 4f)
            circle(13f, 10.5f, 5f)
            circle(17.5f, 14f, 3.6f)
            drawRect(color, p(8f, 13f), Size(10f * unit, 5f * unit))
        }
        Glyph.DOWNLOAD -> {
            line(12f, 3.5f, 12f, 15f)
            path(listOf(6.5f to 9.5f, 12f to 15.5f, 17.5f to 9.5f))
            path(listOf(4f to 17f, 4f to 20.5f, 20f to 20.5f, 20f to 17f))
        }
        Glyph.UPLOAD -> {
            line(12f, 15f, 12f, 3.5f)
            path(listOf(6.5f to 9f, 12f to 3f, 17.5f to 9f))
            path(listOf(4f to 17f, 4f to 20.5f, 20f to 20.5f, 20f to 17f))
        }
        Glyph.REFRESH -> {
            arc(12f, 12f, 8f, 30f, 280f)
            path(listOf(17.3f to 3.8f, 20.4f to 8.4f, 15f to 8.7f), closed = true)
        }
        Glyph.ZOOM_IN, Glyph.ZOOM_OUT -> {
            circle(10.5f, 10.5f, 6f)
            line(15f, 15f, 20f, 20f, width = 2f * unit)
            line(7.8f, 10.5f, 13.2f, 10.5f)
            if (glyph == Glyph.ZOOM_IN) line(10.5f, 7.8f, 10.5f, 13.2f)
        }
        Glyph.ROTATE -> {
            arc(12f, 12f, 7.8f, 30f, 270f)
            path(listOf(18.5f to 17.5f, 18.7f to 12f, 13.4f to 13.6f), closed = false)
        }
        Glyph.MOVE -> {
            line(12f, 2.5f, 12f, 21.5f)
            line(2.5f, 12f, 21.5f, 12f)
            path(listOf(9f to 5.5f, 12f to 2.5f, 15f to 5.5f))
            path(listOf(9f to 18.5f, 12f to 21.5f, 15f to 18.5f))
            path(listOf(5.5f to 9f, 2.5f to 12f, 5.5f to 15f))
            path(listOf(18.5f to 9f, 21.5f to 12f, 18.5f to 15f))
        }
        Glyph.SCALE -> {
            line(5f, 19f, 19f, 5f)
            path(listOf(12f to 5f, 19f to 5f, 19f to 12f))
            path(listOf(5f to 12f, 5f to 19f, 12f to 19f))
        }
        Glyph.SNAP -> {
            path(listOf(5f to 3f, 5f to 11f, 8.5f to 15f, 8.5f to 21f))
            path(listOf(19f to 3f, 19f to 11f, 15.5f to 15f, 15.5f to 21f))
            line(5f, 3f, 8.5f, 3f)
            line(15.5f, 3f, 19f, 3f)
        }
        Glyph.CODE -> {
            path(listOf(8.5f to 4f, 3f to 12f, 8.5f to 20f))
            path(listOf(15.5f to 4f, 21f to 12f, 15.5f to 20f))
        }
        Glyph.TERMINAL -> {
            box(3f, 4.5f, 18f, 15f, 1.6f)
            path(listOf(7f to 9f, 10.5f to 12f, 7f to 15f))
            line(12.5f, 15f, 17f, 15f)
        }
        Glyph.EYE -> {
            path(listOf(2.5f to 12f, 8f to 6.5f, 16f to 6.5f, 21.5f to 12f, 16f to 17.5f, 8f to 17.5f), closed = true)
            circle(12f, 12f, 2.8f, filled = true)
        }
        Glyph.LOCK -> {
            box(5f, 10.5f, 14f, 10f, 2f)
            arc(12f, 10.5f, 4.4f, 180f, 180f)
            circle(12f, 15f, 1.2f, filled = true)
            line(12f, 15f, 12f, 17.5f)
        }
        Glyph.WELD -> {
            arc(9f, 12f, 4.2f, 40f, 220f)
            arc(15f, 12f, 4.2f, 220f, 220f)
            line(9.2f, 10f, 14.8f, 14f)
        }
        Glyph.WORLD -> {
            circle(12f, 12f, 8.6f)
            line(3.5f, 12f, 20.5f, 12f)
            arc(12f, 12f, 3.6f, 90f, 180f)
            arc(12f, 12f, 3.6f, 270f, 180f)
        }
        Glyph.CAMERA -> {
            box(3f, 7f, 18f, 12f, 2f)
            path(listOf(8f to 7f, 10f to 4.5f, 14f to 4.5f, 16f to 7f))
            circle(12f, 13f, 3.2f)
        }
        Glyph.LAYERS -> {
            path(listOf(12f to 3f, 21f to 8f, 12f to 13f, 3f to 8f), closed = true)
            path(listOf(3f to 13f, 12f to 18f, 21f to 13f))
            path(listOf(3f to 17.5f, 12f to 22f, 21f to 17.5f))
        }
        Glyph.DICE -> {
            box(3.5f, 3.5f, 17f, 17f, 3f)
            circle(8.5f, 8.5f, 1.2f, filled = true)
            circle(15.5f, 8.5f, 1.2f, filled = true)
            circle(12f, 12f, 1.2f, filled = true)
            circle(8.5f, 15.5f, 1.2f, filled = true)
            circle(15.5f, 15.5f, 1.2f, filled = true)
        }
        Glyph.STAR -> path(
            listOf(
                12f to 2.5f, 14.7f to 8.6f, 21.5f to 9.3f, 16.4f to 13.8f,
                17.9f to 20.5f, 12f to 17f, 6.1f to 20.5f, 7.6f to 13.8f,
                2.5f to 9.3f, 9.3f to 8.6f
            ),
            closed = true, filled = true
        )
        Glyph.FLAG -> {
            line(6f, 3f, 6f, 21f)
            path(listOf(6f to 4f, 19f to 7f, 6f to 11f), closed = true, filled = true)
        }
        Glyph.USER_PLUS -> {
            circle(9.5f, 8.5f, 3.4f)
            arc(9.5f, 19f, 6f, 200f, 140f)
            line(18f, 8f, 18f, 14f)
            line(15f, 11f, 21f, 11f)
        }
        Glyph.LOGOUT -> {
            box(4f, 4f, 9f, 16f, 1.6f)
            line(11f, 12f, 21f, 12f)
            path(listOf(17f to 8f, 21f to 12f, 17f to 16f))
        }
        Glyph.FILTER -> {
            line(3.5f, 6f, 20.5f, 6f)
            line(6.5f, 12f, 17.5f, 12f)
            line(10f, 18f, 14f, 18f)
        }
        Glyph.MORE -> {
            circle(6f, 12f, 1.4f, filled = true)
            circle(12f, 12f, 1.4f, filled = true)
            circle(18f, 12f, 1.4f, filled = true)
        }
        Glyph.DRAG -> {
            circle(8f, 6f, 1.2f, filled = true)
            circle(16f, 6f, 1.2f, filled = true)
            circle(8f, 12f, 1.2f, filled = true)
            circle(16f, 12f, 1.2f, filled = true)
            circle(8f, 18f, 1.2f, filled = true)
            circle(16f, 18f, 1.2f, filled = true)
        }
    }
}
