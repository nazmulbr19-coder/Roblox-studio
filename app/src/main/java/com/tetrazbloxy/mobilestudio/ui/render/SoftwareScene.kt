package com.tetrazbloxy.mobilestudio.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tetrazbloxy.mobilestudio.model.BasePart
import com.tetrazbloxy.mobilestudio.model.CFrame
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class Projected(val x: Float, val y: Float, val depth: Float)

class SceneCamera(
    val eye: Vector3,
    val target: Vector3
) {
    private val forward: Vector3
    private val right: Vector3
    private val up: Vector3

    init {
        val raw = target - eye
        forward = if (raw.magnitude < 1e-5f) Vector3(0f, 0f, -1f) else raw.unit
        val rightRaw = forward.cross(Vector3(0f, 1f, 0f))
        right = if (rightRaw.magnitude < 1e-4f) Vector3(1f, 0f, 0f) else rightRaw.unit
        up = right.cross(forward).unit
    }

    fun project(point: Vector3, width: Float, height: Float, focalScale: Float = 1.55f): Projected? {
        val delta = point - eye
        val cameraZ = delta.dot(forward)
        if (cameraZ <= 0.05f) return null
        val focal = minOf(width, height) * focalScale
        val cameraX = delta.dot(right)
        val cameraY = delta.dot(up)
        return Projected(
            x = width * 0.5f + cameraX / cameraZ * focal,
            y = height * 0.5f - cameraY / cameraZ * focal,
            depth = cameraZ
        )
    }
}

private data class Face(
    val points: List<Vector3>,
    val normal: Vector3,
    val center: Vector3,
    val color: Color,
    val depth: Float
)

object SoftwareScene {
    fun cameraForOrbit(target: Vector3, yaw: Float, pitch: Float, distance: Float): SceneCamera {
        val forward = Vector3(
            -sin(yaw) * cos(pitch),
            sin(pitch),
            -cos(yaw) * cos(pitch)
        )
        return SceneCamera(target - forward * distance, target)
    }

    fun partsOf(document: DataModel): List<BasePart> =
        document.workspace.getDescendants().filterIsInstance<BasePart>()

    fun draw(
        drawScope: DrawScope,
        camera: SceneCamera,
        parts: List<BasePart>,
        selected: Set<BasePart>,
        accent: Color,
        background: Color,
        showGrid: Boolean,
        characterPosition: Vector3? = null,
        characterYaw: Float = 0f,
        wireframe: Boolean = false
    ) = with(drawScope) {
        drawRect(background)

        if (showGrid) {
            drawGroundGrid(camera)
        }

        val faces = mutableListOf<Face>()
        for (part in parts) {
            buildPartFaces(part, camera, faces)
        }
        characterPosition?.let { buildCharacterFaces(it, characterYaw, camera, faces) }

        faces.sortByDescending { it.depth }

        for (face in faces) {
            val projected = face.points.map { camera.project(it, size.width, size.height) ?: return@map null }
            if (projected.any { it == null }) continue
            val points = projected.filterNotNull()
            if (points.size < 3) continue
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            for (index in 1 until points.size) path.lineTo(points[index].x, points[index].y)
            path.close()

            if (wireframe) {
                drawPath(path, face.color.copy(alpha = 0.35f), style = Stroke(width = 1.2f))
            } else {
                drawPath(path, face.color)
                drawPath(path, face.color.copy(alpha = 0.35f), style = Stroke(width = 0.7f))
            }
        }

        for (part in selected) {
            drawPartOutline(camera, part, accent)
        }
    }

    private fun DrawScope.drawGroundGrid(camera: SceneCamera) {
        val extent = 256
        val step = 16
        var value = -extent
        while (value <= extent) {
            val alpha = if (value == 0) 0.42f else 0.13f
            val color = when (value) {
                0 -> Color(0xFF5B7DB1).copy(alpha = 0.45f)
                else -> Color(0xFF8A8F9C).copy(alpha = alpha)
            }
            drawWorldLine(camera, Vector3(value.toFloat(), 0f, -extent.toFloat()), Vector3(value.toFloat(), 0f, extent.toFloat()), color)
            drawWorldLine(camera, Vector3(-extent.toFloat(), 0f, value.toFloat()), Vector3(extent.toFloat(), 0f, value.toFloat()), color)
            value += step
        }
    }

    private fun DrawScope.drawWorldLine(
        camera: SceneCamera,
        a: Vector3,
        b: Vector3,
        color: Color
    ) {
        val pa = camera.project(a, size.width, size.height) ?: return
        val pb = camera.project(b, size.width, size.height) ?: return
        drawLine(color, Offset(pa.x, pa.y), Offset(pb.x, pb.y), strokeWidth = 1f, cap = StrokeCap.Round)
    }

    private fun DrawScope.drawPartOutline(camera: SceneCamera, part: BasePart, color: Color) {
        val points = corners(part.cframe, part.size).map {
            camera.project(it, size.width, size.height) ?: return
        }
        val edges = listOf(
            0 to 1, 1 to 3, 3 to 2, 2 to 0,
            4 to 5, 5 to 7, 7 to 6, 6 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )
        for ((start, end) in edges) {
            val a = points[start]
            val b = points[end]
            drawLine(
                color,
                Offset(a.x, a.y),
                Offset(b.x, b.y),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }

    private fun buildPartFaces(part: BasePart, camera: SceneCamera, out: MutableList<Face>) {
        if (part.transparency >= 0.995f) return
        val alpha = (1f - part.transparency).coerceIn(0.05f, 1f)
        val base = Color(part.color.toArgb((alpha * 255).toInt()))
        val corners = corners(part.cframe, part.size)
        addBoxFaces(part.cframe, corners, base, camera, out)
    }

    private fun addBoxFaces(
        frame: CFrame,
        corners: List<Vector3>,
        base: Color,
        camera: SceneCamera,
        out: MutableList<Face>
    ) {
        val faces = listOf(
            listOf(4, 5, 7, 6) to frame.lookVector,
            listOf(0, 1, 3, 2) to -frame.lookVector,
            listOf(4, 0, 2, 6) to -frame.right,
            listOf(1, 5, 7, 3) to frame.right,
            listOf(2, 3, 7, 6) to frame.up,
            listOf(4, 5, 1, 0) to -frame.up
        )
        val light = Vector3(-0.35f, 0.9f, 0.24f).unit
        for ((indices, normal) in faces) {
            val points = indices.map { corners[it] }
            val center = points.fold(Vector3.ZERO) { acc, point -> acc + point } / points.size.toFloat()
            val toEye = camera.eye - center
            if (normal.dot(toEye) <= 0f) continue
            val shade = 0.58f + 0.46f * abs(normal.dot(light))
            val color = base.copy(
                red = (base.red * shade).coerceIn(0f, 1f),
                green = (base.green * shade).coerceIn(0f, 1f),
                blue = (base.blue * shade).coerceIn(0f, 1f)
            )
            val depth = (center - camera.eye).magnitude
            out.add(Face(points, normal, center, color, depth))
        }
    }

    private fun corners(frame: CFrame, sizeVector: Vector3): List<Vector3> {
        val right = frame.right * (sizeVector.x * 0.5f)
        val up = frame.up * (sizeVector.y * 0.5f)
        val look = frame.lookVector * (sizeVector.z * 0.5f)
        val centre = frame.position
        val result = mutableListOf<Vector3>()
        val signs = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f),
            Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f),
            Triple(-1f, -1f, 1f), Triple(1f, -1f, 1f),
            Triple(-1f, 1f, 1f), Triple(1f, 1f, 1f)
        )
        for ((x, y, z) in signs) {
            result.add(centre + right * x + up * y + look * z)
        }
        return result
    }

    private fun buildCharacterFaces(
        feet: Vector3,
        yaw: Float,
        camera: SceneCamera,
        out: MutableList<Face>
    ) {
        val rotation = CFrame.Angles(0f, yaw, 0f)
        val root = CFrame.new(feet.x, feet.y, feet.z) * rotation
        fun limb(offset: Vector3, sizeVector: Vector3, color: Color) {
            val frame = root * CFrame.new(offset)
            val corners = corners(frame, sizeVector)
            addBoxFaces(frame, corners, color, camera, out)
        }
        limb(Vector3(0f, 3.1f, 0f), Vector3(2.4f, 2.3f, 1.2f), Color(0xFF3B82F6))
        limb(Vector3(0f, 4.9f, 0f), Vector3(1.8f, 1.8f, 1.8f), Color(0xFFF5D048))
        limb(Vector3(-0.7f, 1.15f, 0f), Vector3(1f, 1.9f, 1f), Color(0xFF4B974B))
        limb(Vector3(0.7f, 1.15f, 0f), Vector3(1f, 1.9f, 1f), Color(0xFF4B974B))
        limb(Vector3(-1.7f, 3.2f, 0f), Vector3(0.9f, 1.9f, 0.9f), Color(0xFFF5D048))
        limb(Vector3(1.7f, 3.2f, 0f), Vector3(0.9f, 1.9f, 0.9f), Color(0xFFF5D048))
    }

    fun pickPart(
        camera: SceneCamera,
        parts: List<BasePart>,
        width: Float,
        height: Float,
        tapX: Float,
        tapY: Float
    ): BasePart? {
        var best: BasePart? = null
        var bestScore = Float.MAX_VALUE
        for (part in parts) {
            val center = camera.project(part.cframe.position, width, height) ?: continue
            val edge = camera.project(
                part.cframe.position + part.cframe.right * (part.size.x * 0.5f),
                width,
                height
            )
            val radius = if (edge == null) 24f else {
                abs(edge.x - center.x).coerceIn(10f, 220f)
            }
            val dx = center.x - tapX
            val dy = center.y - tapY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance > radius + 22f) continue
            val score = distance + center.depth * 0.05f
            if (score < bestScore) {
                bestScore = score
                best = part
            }
        }
        return best
    }
}
