package com.tetrazbloxy.mobilestudio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoManagerTest {
    @Test
    fun recordThenUndoAndRedo() {
        val manager = UndoManager()
        var value = 0
        manager.record(undo = { value = 0 }, redo = { value = 1 })

        value = 1
        assertTrue(manager.canUndo)
        assertTrue(manager.undo())
        assertEquals(0, value)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)

        assertTrue(manager.redo())
        assertEquals(1, value)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
    }

    @Test
    fun batchedOperationsUndoInReverse() {
        val manager = UndoManager()
        var value = 0

        manager.begin("batch")
        manager.record(undo = { value -= 1 }, redo = { value += 1 })
        manager.record(undo = { value -= 10 }, redo = { value += 10 })
        manager.commit()

        value = 11
        assertTrue(manager.undo())
        assertEquals(0, value)

        assertTrue(manager.redo())
        assertEquals(11, value)
    }

    @Test
    fun multipleRedoStepsArePreserved() {
        val manager = UndoManager()
        var value = 0
        manager.record({ value = 0 }, { value = 1 })
        manager.record({ value = 1 }, { value = 2 })
        value = 2

        assertTrue(manager.undo())
        assertEquals(1, value)
        assertTrue(manager.undo())
        assertEquals(0, value)

        assertTrue(manager.redo())
        assertEquals(1, value)
        assertTrue(manager.redo())
        assertEquals(2, value)
    }

    @Test
    fun suspendedRecordingIsIgnored() {
        val manager = UndoManager()
        manager.suspended = true
        manager.record({ }, { })
        assertFalse(manager.canUndo)

        manager.suspended = false
        manager.record({ }, { })
        assertTrue(manager.canUndo)
    }

    @Test
    fun applyRunsRedoAndRecords() {
        val manager = UndoManager()
        var value = 0
        manager.apply(undo = { value = 0 }, redo = { value = 5 })
        assertEquals(5, value)
        assertTrue(manager.undo())
        assertEquals(0, value)
    }

    @Test
    fun limitDropsOldestBatches() {
        val manager = UndoManager(limit = 2)
        manager.record({ }, { })
        manager.record({ }, { })
        manager.record({ }, { })

        assertTrue(manager.undo())
        assertTrue(manager.undo())
        assertFalse(manager.undo())
    }

    @Test
    fun listenerSeesHistoryState() {
        val manager = UndoManager()
        var lastCanUndo = false
        var lastCanRedo = false
        manager.listener = object : UndoManager.Listener {
            override fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) {
                lastCanUndo = canUndo
                lastCanRedo = canRedo
            }
        }

        manager.record({ }, { })
        assertTrue(lastCanUndo)
        assertFalse(lastCanRedo)

        manager.undo()
        assertFalse(lastCanUndo)
        assertTrue(lastCanRedo)
    }

    @Test
    fun clearEmptiesBothStacks() {
        val manager = UndoManager()
        manager.record({ }, { })
        manager.undo()
        manager.clear()
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)
    }
}
