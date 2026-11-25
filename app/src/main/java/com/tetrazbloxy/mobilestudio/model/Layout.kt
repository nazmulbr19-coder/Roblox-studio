package com.tetrazbloxy.mobilestudio.model

/** A scale + pixel-offset pair along one axis (Roblox UDim). */
data class UDim(val scale: Float, val offset: Float) {
    operator fun plus(other: UDim) = UDim(scale + other.scale, offset + other.offset)
    operator fun minus(other: UDim) = UDim(scale - other.scale, offset - other.offset)

    companion object {
        val ZERO = UDim(0f, 0f)
        fun new(scale: Float, offset: Float) = UDim(scale, offset)
    }
}

/** Horizontal + vertical UDim pair (Roblox UDim2). */
data class UDim2(val x: UDim, val y: UDim) {
    val width: Float get() = x.scale
    val height: Float get() = y.scale

    operator fun plus(other: UDim2) = UDim2(x + other.x, y + other.y)
    operator fun minus(other: UDim2) = UDim2(x - other.x, y - other.y)

    companion object {
        val ZERO = UDim2(UDim.ZERO, UDim.ZERO)

        fun new(scaleX: Float, offsetX: Float, scaleY: Float, offsetY: Float): UDim2 =
            UDim2(UDim(scaleX, offsetX), UDim(scaleY, offsetY))

        fun fromScale(x: Float, y: Float): UDim2 = new(x, 0f, y, 0f)

        fun fromOffset(x: Float, y: Float): UDim2 = new(0f, x, 0f, y)
    }
}

/** Axis-aligned rectangle in 2D space. */
data class Rect(val min: Vector2, val max: Vector2) {
    val width: Float get() = max.x - min.x
    val height: Float get() = max.y - min.y
    val center: Vector2 get() = Vector2((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f)

    fun contains(point: Vector2): Boolean =
        point.x >= min.x && point.x <= max.x && point.y >= min.y && point.y <= max.y

    companion object {
        fun new(minX: Float, minY: Float, maxX: Float, maxY: Float): Rect =
            Rect(Vector2(minX, minY), Vector2(maxX, maxY))
    }
}

/** Inclusive numeric interval (Roblox NumberRange). */
data class NumberRange(val min: Float, val max: Float) {
    companion object {
        fun new(min: Float, max: Float) = NumberRange(min, max)
        fun new(value: Float) = NumberRange(value, value)
    }
}
