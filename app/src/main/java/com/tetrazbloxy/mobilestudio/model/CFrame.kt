package com.tetrazbloxy.mobilestudio.model

import kotlin.math.cos
import kotlin.math.sin

/**
 * Immutable coordinate frame: a position plus a row-major 3x3 rotation matrix.
 * Translation is stored in x/y/z, rotation in r00..r22.
 */
data class CFrame(
    val x: Float,
    val y: Float,
    val z: Float,
    val r00: Float,
    val r01: Float,
    val r02: Float,
    val r10: Float,
    val r11: Float,
    val r12: Float,
    val r20: Float,
    val r21: Float,
    val r22: Float
) {
    val position: Vector3 get() = Vector3(x, y, z)

    /** Local +X axis in world space. */
    val right: Vector3 get() = Vector3(r00, r10, r20)

    /** Local +Y axis in world space. */
    val up: Vector3 get() = Vector3(r01, r11, r21)

    /** Roblox convention: the look direction is local -Z. */
    val lookVector: Vector3 get() = Vector3(-r02, -r12, -r22)

    fun toWorldSpace(point: Vector3): Vector3 = Vector3(
        r00 * point.x + r01 * point.y + r02 * point.z + x,
        r10 * point.x + r11 * point.y + r12 * point.z + y,
        r20 * point.x + r21 * point.y + r22 * point.z + z
    )

    fun toObjectSpace(point: Vector3): Vector3 {
        val dx = point.x - x
        val dy = point.y - y
        val dz = point.z - z
        return Vector3(
            r00 * dx + r10 * dy + r20 * dz,
            r01 * dx + r11 * dy + r21 * dz,
            r02 * dx + r12 * dy + r22 * dz
        )
    }

    fun pointToWorldSpace(point: Vector3): Vector3 = toWorldSpace(point)

    fun pointToObjectSpace(point: Vector3): Vector3 = toObjectSpace(point)

    fun vectorToWorldSpace(vector: Vector3): Vector3 = Vector3(
        r00 * vector.x + r01 * vector.y + r02 * vector.z,
        r10 * vector.x + r11 * vector.y + r12 * vector.z,
        r20 * vector.x + r21 * vector.y + r22 * vector.z
    )

    fun vectorToObjectSpace(vector: Vector3): Vector3 = Vector3(
        r00 * vector.x + r10 * vector.y + r20 * vector.z,
        r01 * vector.x + r11 * vector.y + r21 * vector.z,
        r02 * vector.x + r12 * vector.y + r22 * vector.z
    )

    operator fun times(other: CFrame): CFrame {
        val m00 = r00 * other.r00 + r01 * other.r10 + r02 * other.r20
        val m01 = r00 * other.r01 + r01 * other.r11 + r02 * other.r21
        val m02 = r00 * other.r02 + r01 * other.r12 + r02 * other.r22
        val m10 = r10 * other.r00 + r11 * other.r10 + r12 * other.r20
        val m11 = r10 * other.r01 + r11 * other.r11 + r12 * other.r21
        val m12 = r10 * other.r02 + r11 * other.r12 + r12 * other.r22
        val m20 = r20 * other.r00 + r21 * other.r10 + r22 * other.r20
        val m21 = r20 * other.r01 + r21 * other.r11 + r22 * other.r21
        val m22 = r20 * other.r02 + r21 * other.r12 + r22 * other.r22
        val p = toWorldSpace(other.position)
        return CFrame(p.x, p.y, p.z, m00, m01, m02, m10, m11, m12, m20, m21, m22)
    }

    operator fun times(vector: Vector3): Vector3 = toWorldSpace(vector)

    operator fun plus(offset: Vector3): CFrame = copy(x = x + offset.x, y = y + offset.y, z = z + offset.z)

    operator fun minus(offset: Vector3): CFrame = copy(x = x - offset.x, y = y - offset.y, z = z - offset.z)

    fun inverse(): CFrame {
        val nx = -(r00 * x + r10 * y + r20 * z)
        val ny = -(r01 * x + r11 * y + r21 * z)
        val nz = -(r02 * x + r12 * y + r22 * z)
        return CFrame(
            nx, ny, nz,
            r00, r10, r20,
            r01, r11, r21,
            r02, r12, r22
        )
    }

    fun lerp(target: CFrame, alpha: Float): CFrame = CFrame(
        x + (target.x - x) * alpha,
        y + (target.y - y) * alpha,
        z + (target.z - z) * alpha,
        r00 + (target.r00 - r00) * alpha,
        r01 + (target.r01 - r01) * alpha,
        r02 + (target.r02 - r02) * alpha,
        r10 + (target.r10 - r10) * alpha,
        r11 + (target.r11 - r11) * alpha,
        r12 + (target.r12 - r12) * alpha,
        r20 + (target.r20 - r20) * alpha,
        r21 + (target.r21 - r21) * alpha,
        r22 + (target.r22 - r22) * alpha
    )

    override fun toString(): String = "CFrame($position, look=$lookVector)"

    companion object {
        fun new(x: Float = 0f, y: Float = 0f, z: Float = 0f): CFrame =
            CFrame(x, y, z, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

        fun new(position: Vector3): CFrame = new(position.x, position.y, position.z)

        fun fromMatrix(position: Vector3, matrix: FloatArray): CFrame {
            require(matrix.size == 9) { "Rotation matrix must contain 9 values" }
            return CFrame(
                position.x, position.y, position.z,
                matrix[0], matrix[1], matrix[2],
                matrix[3], matrix[4], matrix[5],
                matrix[6], matrix[7], matrix[8]
            )
        }

        fun fromQuat(rotation: Quat, position: Vector3 = Vector3.ZERO): CFrame =
            fromMatrix(position, rotation.normalized().toRotationMatrix())

        /** Compose rotations as Rx * Ry * Rz (radians), matching Roblox CFrame.Angles. */
        fun Angles(rx: Float, ry: Float, rz: Float): CFrame {
            val cx = cos(rx)
            val sx = sin(rx)
            val cy = cos(ry)
            val sy = sin(ry)
            val cz = cos(rz)
            val sz = sin(rz)
            val mx = floatArrayOf(1f, 0f, 0f, 0f, cx, -sx, 0f, sx, cx)
            val my = floatArrayOf(cy, 0f, sy, 0f, 1f, 0f, -sy, 0f, cy)
            val mz = floatArrayOf(cz, -sz, 0f, sz, cz, 0f, 0f, 0f, 1f)
            return fromMatrix(Vector3.ZERO, multiply(mx, multiply(my, mz)))
        }

        fun fromAxisAngle(axis: Vector3, angle: Float): CFrame =
            fromQuat(Quat.fromAxisAngle(axis, angle))

        fun lookAt(from: Vector3, to: Vector3, up: Vector3 = Vector3.Y_AXIS): CFrame {
            val forward = (to - from).unit
            var right = forward.cross(up)
            if (right.magnitude <= 1e-6f) {
                right = forward.cross(Vector3.Z_AXIS)
            }
            right = right.unit
            val trueUp = right.cross(forward)
            val matrix = floatArrayOf(
                right.x, trueUp.x, -forward.x,
                right.y, trueUp.y, -forward.y,
                right.z, trueUp.z, -forward.z
            )
            return fromMatrix(from, matrix)
        }

        private fun multiply(a: FloatArray, b: FloatArray): FloatArray = FloatArray(9) { index ->
            val row = index / 3
            val col = index % 3
            a[row * 3] * b[col] +
                a[row * 3 + 1] * b[3 + col] +
                a[row * 3 + 2] * b[6 + col]
        }
    }
}
