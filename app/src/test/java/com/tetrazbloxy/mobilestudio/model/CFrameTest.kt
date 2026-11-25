package com.tetrazbloxy.mobilestudio.model

import org.junit.Assert.assertTrue
import org.junit.Test

class CFrameTest {
    private val halfPi = (Math.PI / 2.0).toFloat()

    @Test
    fun identityHasExpectedAxes() {
        val cf = CFrame.new()
        assertTrue(cf.right.fuzzyEq(Vector3(1f, 0f, 0f)))
        assertTrue(cf.up.fuzzyEq(Vector3(0f, 1f, 0f)))
        assertTrue(cf.lookVector.fuzzyEq(Vector3(0f, 0f, -1f)))
    }

    @Test
    fun anglesRotateLookVector() {
        val cf = CFrame.Angles(0f, halfPi, 0f)
        val rotated = cf * Vector3(0f, 0f, -1f)
        assertTrue(rotated.fuzzyEq(Vector3(-1f, 0f, 0f), 1e-4f))
    }

    @Test
    fun lookAtFacesTarget() {
        val cf = CFrame.lookAt(Vector3.ZERO, Vector3(0f, 0f, -10f))
        assertTrue(cf.lookVector.fuzzyEq(Vector3(0f, 0f, -1f), 1e-4f))
        assertTrue(cf.position.fuzzyEq(Vector3.ZERO))
    }

    @Test
    fun worldObjectSpaceRoundTrip() {
        val cf = CFrame.Angles(0.3f, 0.7f, -0.2f) + Vector3(12f, -3f, 8f)
        val point = Vector3(1f, 2f, 3f)
        val roundTrip = cf.toObjectSpace(cf.toWorldSpace(point))
        assertTrue(roundTrip.fuzzyEq(point, 1e-3f))
    }

    @Test
    fun inverseCancelsTransform() {
        val cf = CFrame.Angles(0.1f, 1.2f, 0.4f) + Vector3(5f, 6f, 7f)
        val combined = cf.inverse() * cf
        assertTrue(combined.position.fuzzyEq(Vector3.ZERO, 1e-3f))
        assertTrue(combined.lookVector.fuzzyEq(Vector3(0f, 0f, -1f), 1e-3f))
    }

    @Test
    fun compositionAddsTranslations() {
        val result = CFrame.new(1f, 0f, 0f) * CFrame.new(0f, 1f, 0f)
        assertTrue(result.position.fuzzyEq(Vector3(1f, 1f, 0f)))
    }

    @Test
    fun axisAngleRotationMovesRightAxis() {
        val cf = CFrame.fromAxisAngle(Vector3.Z_AXIS, halfPi)
        assertTrue(cf.right.fuzzyEq(Vector3(0f, 1f, 0f), 1e-4f))
    }

    @Test
    fun translationOperators() {
        val cf = CFrame.new(1f, 2f, 3f) + Vector3(1f, 1f, 1f)
        assertTrue(cf.position.fuzzyEq(Vector3(2f, 3f, 4f)))
        assertTrue((cf - Vector3(1f, 1f, 1f)).position.fuzzyEq(Vector3(1f, 2f, 3f)))
    }
}
