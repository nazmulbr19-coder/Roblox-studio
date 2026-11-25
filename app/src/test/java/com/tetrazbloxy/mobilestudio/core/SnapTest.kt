package com.tetrazbloxy.mobilestudio.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapTest {
    @Test
    fun snapsToStep() {
        assertEquals(1f, Snap.snap(1.4f, 1f), 1e-6f)
        assertEquals(2f, Snap.snap(1.5f, 1f), 1e-6f)
        assertEquals(7.5f, Snap.snap(7.3f, 0.5f), 1e-6f)
        assertEquals(-2f, Snap.snap(-1.6f, 1f), 1e-6f)
    }

    @Test
    fun zeroStepLeavesValueUntouched() {
        assertEquals(3.14f, Snap.snap(3.14f, 0f), 1e-6f)
    }

    @Test
    fun integerSnapping() {
        assertEquals(3, Snap.snapToInt(2.6f, 1))
        assertEquals(4, Snap.snapToInt(3.6f, 2))
        assertEquals(0, Snap.snapToInt(0.4f, 0))
    }

    @Test
    fun doubleSnapping() {
        assertEquals(2.5, Snap.snap(2.4, 0.5), 1e-9)
        assertEquals(2.0, Snap.snap(1.9, 1.0), 1e-9)
    }
}
