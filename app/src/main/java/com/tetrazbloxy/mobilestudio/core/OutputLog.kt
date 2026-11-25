package com.tetrazbloxy.mobilestudio.core

import java.util.concurrent.CopyOnWriteArrayList

/** In-memory output log with level, snapshot, and listener support. */
class OutputLog {
    enum class Level { INFO, WARN, ERROR, PRINT }

    data class Line(val level: Level, val text: String, val time: Long)

    private val lines = mutableListOf<Line>()
    private val listeners = CopyOnWriteArrayList<(Line) -> Unit>()

    fun log(level: Level, text: String) {
        val line = Line(level, text, System.currentTimeMillis())
        synchronized(lines) {
            lines.add(line)
            while (lines.size > 2000) {
                lines.removeAt(0)
            }
        }
        for (listener in listeners) {
            listener(line)
        }
    }

    fun info(text: String) = log(Level.INFO, text)
    fun warn(text: String) = log(Level.WARN, text)
    fun error(text: String) = log(Level.ERROR, text)
    fun print(text: String) = log(Level.PRINT, text)

    fun snapshot(): List<Line> = synchronized(lines) { lines.toList() }

    fun clear() {
        synchronized(lines) { lines.clear() }
    }

    fun addListener(listener: (Line) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun removeListener(listener: (Line) -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        val INSTANCE = OutputLog()
    }
}
