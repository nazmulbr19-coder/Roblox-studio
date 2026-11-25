package com.tetrazbloxy.mobilestudio.core

import java.util.ArrayDeque

/**
 * Command-pattern undo/redo with batched operations.
 * Callbacks are executed with [suspended] temporarily set so that undo/redo
 * itself never records new history.
 */
class UndoManager(private val limit: Int = 200) {
    interface Listener {
        fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean)
    }

    private data class Op(val undo: () -> Unit, val redo: () -> Unit)

    private class Batch(val label: String) {
        val ops = mutableListOf<Op>()
    }

    private val undoStack = ArrayDeque<Batch>()
    private val redoStack = ArrayDeque<Batch>()
    private var openBatch: Batch? = null

    var suspended: Boolean = false
    var listener: Listener? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty() || (openBatch?.ops?.isNotEmpty() == true)
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun begin(label: String) {
        openBatch?.let { commit() }
        openBatch = Batch(label)
    }

    fun record(undo: () -> Unit, redo: () -> Unit) {
        if (suspended) return
        val current = openBatch
        if (current != null) {
            current.ops.add(Op(undo, redo))
            return
        }
        val single = Batch("")
        single.ops.add(Op(undo, redo))
        push(single)
    }

    /** Runs [redo] immediately and records the reversible pair. */
    fun apply(undo: () -> Unit, redo: () -> Unit) {
        redo()
        record(undo, redo)
    }

    fun commit() {
        val current = openBatch ?: return
        openBatch = null
        if (current.ops.isNotEmpty()) {
            push(current)
        }
    }

    fun undo(): Boolean {
        commit()
        val batch = undoStack.pollLast() ?: return false
        val previous = suspended
        suspended = true
        try {
            for (index in batch.ops.indices.reversed()) {
                batch.ops[index].undo()
            }
        } finally {
            suspended = previous
        }
        redoStack.addLast(batch)
        notifyChanged()
        return true
    }

    fun redo(): Boolean {
        commit()
        val batch = redoStack.pollLast() ?: return false
        val previous = suspended
        suspended = true
        try {
            for (operation in batch.ops) {
                operation.redo()
            }
        } finally {
            suspended = previous
        }
        push(batch, clearRedo = false)
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        openBatch = null
        notifyChanged()
    }

    private fun push(batch: Batch, clearRedo: Boolean = true) {
        undoStack.addLast(batch)
        while (undoStack.size > limit) {
            undoStack.removeFirst()
        }
        if (clearRedo) {
            redoStack.clear()
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        listener?.onHistoryChanged(canUndo, canRedo)
    }
}
