package com.tetrazbloxy.mobilestudio.core

import kotlin.math.roundToInt

/** Move/rotate snapping helpers (SPEC: roundToInt(v / s) * s). */
object Snap {
    fun snap(value: Float, step: Float): Float =
        if (step <= 0f) value else (value / step).roundToInt() * step

    fun snap(value: Double, step: Double): Double =
        if (step <= 0.0) value else Math.round(value / step) * step

    fun snapToInt(value: Float, step: Int): Int =
        if (step <= 0) value.roundToInt() else (value / step).roundToInt() * step

    fun snapToInt(value: Double, step: Int): Int =
        if (step <= 0) Math.round(value).toInt() else Math.round(value / step).toInt() * step
}
