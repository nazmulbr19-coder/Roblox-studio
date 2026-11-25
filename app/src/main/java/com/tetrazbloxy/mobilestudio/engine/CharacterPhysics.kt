package com.tetrazbloxy.mobilestudio.engine

import com.tetrazbloxy.mobilestudio.model.Vector3

/** Quantified playtest limits from SPEC sections 8.4 and 10. */
object PlaytestConstants {
    const val DEFAULT_WALK_SPEED = 16f
    const val DEFAULT_JUMP_POWER = 50f
    const val DEFAULT_JUMP_HEIGHT = 7.2f
    const val DEFAULT_GRAVITY = 196.2f
    const val DEFAULT_RESPAWN_TIME = 5f
    const val DEFAULT_FALLEN_PARTS_DESTROY_HEIGHT = -500f
    const val MAX_DT = 0.05f
    const val MOVETO_ARRIVE_DISTANCE = 1.2f
    const val MAX_SUBSTEPS = 12
    const val SUBSTEP_DISTANCE = 0.6f
    const val JUMP_ANIM_TIME = 0.3f
    const val CLIMB_VERTICAL_SPEED = 0.7f
    const val CLIMB_LEAP_VERTICAL = 22f
    const val PLAYER_CHOICE_AVATAR = 2
}

/** Character movement formulas from SPEC 8.4. */
object CharacterPhysics {
    fun jumpVelocity(
        jumpPower: Float = PlaytestConstants.DEFAULT_JUMP_POWER,
        useJumpPower: Boolean = true,
        gravity: Float = PlaytestConstants.DEFAULT_GRAVITY,
        jumpHeight: Float = PlaytestConstants.DEFAULT_JUMP_HEIGHT
    ): Float = if (useJumpPower) jumpPower else kotlin.math.sqrt(2f * gravity * jumpHeight)

    fun applyGravity(
        velocityY: Float,
        gravity: Float = PlaytestConstants.DEFAULT_GRAVITY,
        dt: Float
    ): Float = velocityY - gravity * dt

    /**
     * Camera-yaw-relative horizontal move direction:
     * forward = (-sin(yaw), 0, -cos(yaw)), right = (cos(yaw), 0, -sin(yaw)).
     * The result is normalized only when its magnitude exceeds 1.
     */
    fun moveDirection(yaw: Float, moveX: Float, moveY: Float): Vector3 {
        val fx = -kotlin.math.sin(yaw)
        val fz = -kotlin.math.cos(yaw)
        val rx = kotlin.math.cos(yaw)
        val rz = -kotlin.math.sin(yaw)

        var x = fx * (-moveY) + rx * moveX
        var z = fz * (-moveY) + rz * moveX
        val length = kotlin.math.sqrt(x * x + z * z)
        if (length > 1f) {
            x /= length
            z /= length
        }
        return Vector3(x, 0f, z)
    }

    fun clampDt(dt: Float): Float = dt.coerceIn(0f, PlaytestConstants.MAX_DT)

    /** clamp(ceil(|velocity| * dt / 0.6), 1, 12). */
    fun collisionSubsteps(
        velocity: Vector3,
        dt: Float
    ): Int {
        val raw = kotlin.math.ceil(
            velocity.magnitude * dt / PlaytestConstants.SUBSTEP_DISTANCE
        ).toInt()
        return raw.coerceIn(1, PlaytestConstants.MAX_SUBSTEPS)
    }

    fun moveToReached(distance: Float): Boolean =
        distance <= PlaytestConstants.MOVETO_ARRIVE_DISTANCE

    fun respawnDelay(
        configured: Float?,
        autoLoads: Boolean = true
    ): Float? = if (autoLoads) (configured ?: PlaytestConstants.DEFAULT_RESPAWN_TIME) else null
}
