package com.tetrazbloxy.mobilestudio.engine

import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** One physics raycast result. */
data class RayHit(
    val position: Vector3,
    val normal: Vector3,
    val part: Instance?,
    val t: Float
)

/** Camera formulas from SPEC 8.4 / section 10. */
object CameraMath {
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 400f
    const val DEFAULT_MIN_ZOOM = 0.5f
    const val DEFAULT_MAX_ZOOM = 128f

    const val AUTO_TURN_IDLE_SECONDS = 0.8f
    const val AUTO_TURN_MAX_DIFF = 2.2f
    const val SHIFT_LOCK_OFFSET = 1.75f

    fun wrapAngle(angle: Float): Float {
        val pi = PI.toFloat()
        val twoPi = pi * 2f
        var value = angle % twoPi
        if (value > pi) value -= twoPi
        if (value < -pi) value += twoPi
        return value
    }

    /** Exponential approach: cur + (target - cur) * (1 - exp(-rate * dt)). */
    fun approach(current: Float, target: Float, rate: Float, dt: Float): Float =
        current + (target - current) * (1f - exp(-rate * dt))

    fun shouldAutoTurn(
        idleSeconds: Float,
        grounded: Boolean,
        moving: Boolean,
        diff: Float
    ): Boolean = idleSeconds > AUTO_TURN_IDLE_SECONDS &&
        grounded &&
        moving &&
        abs(diff) < AUTO_TURN_MAX_DIFF

    fun autoTurnYaw(yaw: Float, moveDirectionYaw: Float, dt: Float): Float {
        val diff = wrapAngle(moveDirectionYaw - yaw)
        if (abs(diff) >= AUTO_TURN_MAX_DIFF) return yaw
        return yaw + diff * (1f - exp(-0.9f * dt))
    }

    fun clampZoomMin(value: Float): Float = value.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun clampZoomMax(value: Float): Float = value.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun zoomLimits(
        min: Float = DEFAULT_MIN_ZOOM,
        max: Float = DEFAULT_MAX_ZOOM
    ): Pair<Float, Float> {
        val lo = clampZoomMin(min)
        val hi = clampZoomMax(max)
        return if (lo <= hi) lo to hi else hi to lo
    }

    fun clampFov(fov: Float): Float = fov.coerceIn(1f, 120f)

    fun shiftLockOffset(right: Vector3): Vector3 = right * SHIFT_LOCK_OFFSET

    /** Third-person camera distance allowed up to a raycast hit. */
    fun thirdPersonDistance(hitT: Float): Float = max(hitT - 0.4f, 0.5f)

    fun occlusionRecovery(current: Float, target: Float, dt: Float): Float =
        approach(current, target, 6f, dt)

    fun subjectHeightFollow(current: Float, target: Float, dt: Float): Float =
        approach(current, target, 18f, dt)
}
