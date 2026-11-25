package com.tetrazbloxy.mobilestudio.engine

import com.tetrazbloxy.mobilestudio.model.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class CameraMathTest {
    private val pi = PI.toFloat()

    @Test
    fun wrapAngleKeepsRange() {
        assertEquals(0.5f, CameraMath.wrapAngle(0.5f), 1e-5f)
        assertEquals(pi, CameraMath.wrapAngle(3f * pi), 1e-4f)
        assertEquals(-pi, CameraMath.wrapAngle(-3f * pi), 1e-4f)
        assertEquals(0f, CameraMath.wrapAngle(2f * pi), 1e-4f)
    }

    @Test
    fun approachUsesExponentialSmoothing() {
        assertEquals(6.3212f, CameraMath.approach(0f, 10f, 1f, 1f), 1e-3f)
        assertEquals(0f, CameraMath.approach(0f, 10f, 1f, 0f), 1e-5f)
    }

    @Test
    fun autoTurnOnlyAfterIdleGroundedMoving() {
        assertTrue(CameraMath.shouldAutoTurn(1f, true, true, 1f))
        assertFalse(CameraMath.shouldAutoTurn(0.5f, true, true, 1f))
        assertFalse(CameraMath.shouldAutoTurn(1f, false, true, 1f))
        assertFalse(CameraMath.shouldAutoTurn(1f, true, false, 1f))
        assertFalse(CameraMath.shouldAutoTurn(1f, true, true, 2.5f))
    }

    @Test
    fun autoTurnEasesTowardMovement() {
        val result = CameraMath.autoTurnYaw(0f, 0.1f, 1f)
        assertTrue(result > 0f)
        assertTrue(result < 0.1f)
        assertEquals(0f, CameraMath.autoTurnYaw(0f, 3f, 1f), 1e-6f)
    }

    @Test
    fun zoomLimitsAreClampedAndOrdered() {
        assertEquals(0.5f to 400f, CameraMath.zoomLimits(0.1f, 1000f))
        assertEquals(1f to 100f, CameraMath.zoomLimits(100f, 1f))
        assertEquals(0.5f to 128f, CameraMath.zoomLimits())
    }

    @Test
    fun fovIsClamped() {
        assertEquals(1f, CameraMath.clampFov(0f), 1e-6f)
        assertEquals(120f, CameraMath.clampFov(200f), 1e-6f)
        assertEquals(70f, CameraMath.clampFov(70f), 1e-6f)
    }

    @Test
    fun thirdPersonDistanceRespectsMinimum() {
        assertEquals(0.5f, CameraMath.thirdPersonDistance(0.2f), 1e-6f)
        assertEquals(1.6f, CameraMath.thirdPersonDistance(2f), 1e-6f)
    }

    @Test
    fun shiftLockOffsetAndRecovery() {
        assertEquals(Vector3(1.75f, 0f, 0f), CameraMath.shiftLockOffset(Vector3(1f, 0f, 0f)))
        assertTrue(CameraMath.occlusionRecovery(0f, 10f, 1f) > 9.9f)
        assertTrue(CameraMath.subjectHeightFollow(0f, 10f, 1f) > 9.99f)
    }
}
