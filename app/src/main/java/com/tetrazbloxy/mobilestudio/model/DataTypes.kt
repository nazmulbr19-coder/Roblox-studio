package com.tetrazbloxy.mobilestudio.model

import java.util.Locale
import kotlin.math.abs

/** Number formatting used by property serialization and the property panel. */
object DataTypes {
    fun fmtNum(value: Float): String = fmtNum(value.toDouble())

    fun fmtNum(value: Double): String {
        if (value.isNaN()) return "nan"
        if (value.isInfinite()) return if (value > 0) "inf" else "-inf"
        if (value == Math.floor(value) && abs(value) < 1e15) {
            return value.toLong().toString()
        }
        val text = String.format(Locale.US, "%.6f", value)
            .trimEnd('0')
            .trimEnd('.')
        return if (text == "-0") "0" else text
    }
}
