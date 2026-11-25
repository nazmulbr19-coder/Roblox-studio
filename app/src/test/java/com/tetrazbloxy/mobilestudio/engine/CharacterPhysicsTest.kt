package com.tetrazbloxy.mobilestudio.engine

import com.tetrazbloxy.mobilestudio.model.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterPhysicsTest {
    @Test
    fun jumpPowerIsUsedDirectly() {
        assertEquals(50f, CharacterPhysics.jumpVelocity(), 1e-6f)
        assertEquals(35f, CharacterPhysics.jumpVelocity(jumpPower = 35f), 1e-6f)
    }

    @Test
    fun jumpHeightProducesDefaultEquivalentVelocity() {
        val velocity = CharacterPhysics.jumpVelocity(useJumpPower = false)
        // sqrt(2 * 196.2 * 7.2) ~= 53.15
        assertEquals(53.15f, velocity, 0.05f)
    }

    @Test
    fun gravityReducesVerticalVelocity() {
        assertEquals(-98.1f, CharacterPhysics.applyGravity(0f, 196.2f, 0.5f), 1e-4f)
        assertEquals(80.38f, CharacterPhysics.applyGravity(100f, 196.2f, 0.1f), 1e-4f)
    }

    @Test
    fun moveDirectionFollowsSpecFormula() {
        // yaw = 0: forward axis = (0,0,-1), and the formula applies *(-moveY).
        assertTrue(CharacterPhysics.moveDirection(0f, 0f, 1f).fuzzyEq(Vector3(0f, 0f, 1f)))
        assertTrue(CharacterPhysics.moveDirection(0f, 1f, 0f).fuzzyEq(Vector3(1f, 0f, 0f)))

        val diagonal = CharacterPhysics.moveDirection(0f, 1f, 1f)
        assertEquals(0.7071f, diagonal.x, 1e-3f)
        assertEquals(0.7071f, diagonal.z, 1e-3f)
        assertEquals(0f, diagonal.y, 1e-6f)
    }

    @Test
    fun dtIsClampedToFiftyMilliseconds() {
        assertEquals(0.05f, CharacterPhysics.clampDt(1f), 1e-6f)
        assertEquals(0f, CharacterPhysics.clampDt(-1f), 1e-6f)
        assertEquals(0.016f, CharacterPhysics.clampDt(0.016f), 1e-6f)
    }

    @Test
    fun collisionSubstepsClampBetweenOneAndTwelve() {
        assertEquals(1, CharacterPhysics.collisionSubsteps(Vector3.ZERO, 0.05f))
        assertEquals(9, CharacterPhysics.collisionSubsteps(Vector3(100f, 0f, 0f), 0.05f))
        assertEquals(12, CharacterPhysics.collisionSubsteps(Vector3(1000f, 0f, 0f), 0.05f))
    }

    @Test
    fun moveToArrivesWithinOnePointTwoStuds() {
        assertTrue(CharacterPhysics.moveToReached(1.2f))
        assertTrue(CharacterPhysics.moveToReached(0.5f))
        assertFalse(CharacterPhysics.moveToReached(1.2001f))
    }

    @Test
    fun respawnDelayHonoursAutoLoads() {
        assertEquals(5f, CharacterPhysics.respawnDelay(null)!!, 1e-6f)
        assertEquals(3f, CharacterPhysics.respawnDelay(3f)!!, 1e-6f)
        assertNull(CharacterPhysics.respawnDelay(3f, autoLoads = false))
    }
}
