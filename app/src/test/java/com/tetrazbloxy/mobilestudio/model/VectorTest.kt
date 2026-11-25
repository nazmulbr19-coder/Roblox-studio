package com.tetrazbloxy.mobilestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorTest {
    @Test
    fun magnitudeAndUnit() {
        assertEquals(5f, Vector3(3f, 4f, 0f).magnitude, 1e-5f)
        assertEquals(Vector3(1f, 0f, 0f), Vector3(10f, 0f, 0f).unit)
        assertEquals(Vector3.ZERO, Vector3.ZERO.unit)
    }

    @Test
    fun arithmetic() {
        val a = Vector3(1f, 2f, 3f)
        val b = Vector3(4f, 5f, 6f)
        assertEquals(Vector3(5f, 7f, 9f), a + b)
        assertEquals(Vector3(-3f, -3f, -3f), a - b)
        assertEquals(Vector3(2f, 4f, 6f), a * 2f)
        assertEquals(Vector3(0.5f, 1f, 1.5f), a / 2f)
        assertEquals(32f, a.dot(b), 1e-5f)
    }

    @Test
    fun crossProductFollowsRightHandRule() {
        assertEquals(Vector3(0f, 0f, 1f), Vector3.X_AXIS.cross(Vector3.Y_AXIS))
        assertEquals(Vector3(0f, 0f, -1f), Vector3.Y_AXIS.cross(Vector3.X_AXIS))
    }

    @Test
    fun lerpAndFuzzyEquality() {
        assertEquals(Vector3(5f, 0f, 0f), Vector3(0f, 0f, 0f).lerp(Vector3(10f, 0f, 0f), 0.5f))
        assertTrue(Vector3(1f, 2f, 3f).fuzzyEq(Vector3(1.000001f, 2f, 3f)))
    }

    @Test
    fun vector2Basics() {
        assertEquals(5f, Vector2(3f, 4f).magnitude, 1e-5f)
        assertEquals(Vector2(1f, 1f), Vector2(1f, 0f) + Vector2(0f, 1f))
        assertEquals(2f, Vector2(1f, 0f).cross(Vector2(0f, 2f)), 1e-5f)
    }

    @Test
    fun vector3int16Converts() {
        val value = Vector3int16.new(10, -20, 30)
        assertEquals(10f, value.toVector3().x, 1e-5f)
        assertEquals(-20f, value.toVector3().y, 1e-5f)
        assertEquals(30f, value.toVector3().z, 1e-5f)
    }

    @Test
    fun quaternionIdentityAndAxisRotation() {
        val identity = Quat.IDENTITY.toRotationMatrix()
        assertEquals(1f, identity[0], 1e-5f)
        assertEquals(1f, identity[4], 1e-5f)
        assertEquals(1f, identity[8], 1e-5f)

        val quarterTurn = Quat.fromAxisAngle(Vector3.Y_AXIS, (Math.PI / 2.0).toFloat())
        val matrix = quarterTurn.normalized().toRotationMatrix()
        assertEquals(0f, matrix[0], 1e-5f)
        assertEquals(1f, matrix[2], 1e-5f)
        assertEquals(-1f, matrix[6], 1e-5f)
        assertEquals(0f, matrix[8], 1e-5f)
    }
}
