package com.tetrazbloxy.mobilestudio.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Two-component floating point vector. */
data class Vector2(val x: Float, val y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vector2(x * scale, y * scale)
    operator fun div(scale: Float) = Vector2(x / scale, y / scale)
    operator fun unaryMinus() = Vector2(-x, -y)

    val magnitude: Float get() = sqrt(x * x + y * y)

    val unit: Vector2
        get() {
            val m = magnitude
            return if (m <= 1e-6f) ZERO else this / m
        }

    fun dot(other: Vector2): Float = x * other.x + y * other.y

    fun cross(other: Vector2): Float = x * other.y - y * other.x

    fun lerp(target: Vector2, alpha: Float): Vector2 =
        Vector2(x + (target.x - x) * alpha, y + (target.y - y) * alpha)

    fun fuzzyEq(other: Vector2, epsilon: Float = 1e-5f): Boolean =
        abs(x - other.x) <= epsilon && abs(y - other.y) <= epsilon

    override fun toString(): String = "Vector2($x, $y)"

    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        fun new(x: Float, y: Float) = Vector2(x, y)
    }
}

/** Three-component floating point vector. */
data class Vector3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vector3(x * scale, y * scale, z * scale)
    operator fun div(scale: Float) = Vector3(x / scale, y / scale, z / scale)
    operator fun unaryMinus() = Vector3(-x, -y, -z)

    val magnitude: Float get() = sqrt(x * x + y * y + z * z)

    val unit: Vector3
        get() {
            val m = magnitude
            return if (m <= 1e-6f) ZERO else this / m
        }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun abs(): Vector3 = Vector3(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z))

    fun min(other: Vector3): Vector3 =
        Vector3(kotlin.math.min(x, other.x), kotlin.math.min(y, other.y), kotlin.math.min(z, other.z))

    fun max(other: Vector3): Vector3 =
        Vector3(kotlin.math.max(x, other.x), kotlin.math.max(y, other.y), kotlin.math.max(z, other.z))

    fun lerp(target: Vector3, alpha: Float): Vector3 = Vector3(
        x + (target.x - x) * alpha,
        y + (target.y - y) * alpha,
        z + (target.z - z) * alpha
    )

    fun fuzzyEq(other: Vector3, epsilon: Float = 1e-5f): Boolean =
        abs(x - other.x) <= epsilon && abs(y - other.y) <= epsilon && abs(z - other.z) <= epsilon

    fun toVector2(): Vector2 = Vector2(x, y)

    override fun toString(): String = "Vector3($x, $y, $z)"

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val X_AXIS = Vector3(1f, 0f, 0f)
        val Y_AXIS = Vector3(0f, 1f, 0f)
        val Z_AXIS = Vector3(0f, 0f, 1f)

        fun new(x: Float, y: Float, z: Float) = Vector3(x, y, z)
    }
}

/** Three-component signed 16-bit integer vector (Roblox Vector3int16). */
data class Vector3int16(val x: Short, val y: Short, val z: Short) {
    constructor(x: Int, y: Int, z: Int) : this(x.toShort(), y.toShort(), z.toShort())

    fun toVector3(): Vector3 = Vector3(x.toFloat(), y.toFloat(), z.toFloat())

    companion object {
        val ZERO = Vector3int16(0, 0, 0)
        fun new(x: Int, y: Int, z: Int) = Vector3int16(x, y, z)
    }
}

/** Unit quaternion used by [CFrame] rotation helpers. */
data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {
    operator fun times(other: Quat): Quat = Quat(
        w * other.w - x * other.x - y * other.y - z * other.z,
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w
    )

    fun normalized(): Quat {
        val length = sqrt(w * w + x * x + y * y + z * z)
        return if (length <= 1e-6f) IDENTITY else Quat(w / length, x / length, y / length, z / length)
    }

    /** Row-major 3x3 rotation matrix. */
    fun toRotationMatrix(): FloatArray {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z
        return floatArrayOf(
            1f - 2f * (yy + zz), 2f * (xy - wz), 2f * (xz + wy),
            2f * (xy + wz), 1f - 2f * (xx + zz), 2f * (yz - wx),
            2f * (xz - wy), 2f * (yz + wx), 1f - 2f * (xx + yy)
        )
    }

    companion object {
        val IDENTITY = Quat(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vector3, angle: Float): Quat {
            val a = axis.unit
            val half = angle * 0.5f
            val s = sin(half)
            return Quat(cos(half), a.x * s, a.y * s, a.z * s).normalized()
        }
    }
}
