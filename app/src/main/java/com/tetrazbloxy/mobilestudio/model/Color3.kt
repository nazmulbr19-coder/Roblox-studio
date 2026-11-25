package com.tetrazbloxy.mobilestudio.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** RGB color with 0..1 components. */
data class Color3(val r: Float, val g: Float, val b: Float) {
    val r255: Int get() = (r * 255f).roundToInt().coerceIn(0, 255)
    val g255: Int get() = (g * 255f).roundToInt().coerceIn(0, 255)
    val b255: Int get() = (b * 255f).roundToInt().coerceIn(0, 255)

    fun toArgb(alpha: Int = 255): Int =
        (alpha.coerceIn(0, 255) shl 24) or (r255 shl 16) or (g255 shl 8) or b255

    fun toHex(): String = "#%02X%02X%02X".format(r255, g255, b255)

    fun lerp(target: Color3, alpha: Float): Color3 = Color3(
        r + (target.r - r) * alpha,
        g + (target.g - g) * alpha,
        b + (target.b - b) * alpha
    )

    fun fuzzyEq(other: Color3, epsilon: Float = 1e-4f): Boolean =
        abs(r - other.r) <= epsilon && abs(g - other.g) <= epsilon && abs(b - other.b) <= epsilon

    companion object {
        val WHITE = Color3(1f, 1f, 1f)
        val BLACK = Color3(0f, 0f, 0f)

        fun new(r: Float, g: Float, b: Float) = Color3(r, g, b)

        fun fromRGB(r: Int, g: Int, b: Int): Color3 = Color3(
            r.coerceIn(0, 255) / 255f,
            g.coerceIn(0, 255) / 255f,
            b.coerceIn(0, 255) / 255f
        )

        fun fromArgb(argb: Int): Color3 = fromRGB(
            (argb shr 16) and 0xFF,
            (argb shr 8) and 0xFF,
            argb and 0xFF
        )

        fun fromHex(hex: String): Color3? {
            val text = hex.removePrefix("#")
            if (text.length != 6) return null
            val value = text.toIntOrNull(16) ?: return null
            return fromRGB((value shr 16) and 0xFF, (value shr 8) and 0xFF, value and 0xFF)
        }
    }
}

/** Strongly typed brick color: display name, numeric id, and RGB value. */
data class BrickColorV(val name: String, val number: Int, val color: Color3)

/** Minimal BrickColor palette with name/id/color lookup and nearest-color search. */
object BrickColors {
    private val TABLE: List<BrickColorV> = listOf(
        BrickColorV("White", 1, Color3.fromRGB(242, 243, 243)),
        BrickColorV("Grey", 194, Color3.fromRGB(163, 162, 165)),
        BrickColorV("Dark stone grey", 199, Color3.fromRGB(99, 95, 98)),
        BrickColorV("Black", 21, Color3.fromRGB(27, 42, 53)),
        BrickColorV("Bright red", 21 + 1, Color3.fromRGB(196, 40, 28)),
        BrickColorV("Bright orange", 106, Color3.fromRGB(218, 133, 65)),
        BrickColorV("Bright yellow", 24, Color3.fromRGB(245, 205, 48)),
        BrickColorV("Bright green", 37, Color3.fromRGB(75, 151, 75)),
        BrickColorV("Bright blue", 23, Color3.fromRGB(13, 105, 172)),
        BrickColorV("Bright violet", 104, Color3.fromRGB(107, 50, 124)),
        BrickColorV("Bright pink", 1013, Color3.fromRGB(255, 102, 204)),
        BrickColorV("Brown", 217, Color3.fromRGB(124, 92, 70)),
        BrickColorV("Reddish brown", 192, Color3.fromRGB(105, 64, 40)),
        BrickColorV("Sand yellow", 90, Color3.fromRGB(180, 132, 85)),
        BrickColorV("Nougat", 135, Color3.fromRGB(204, 142, 105)),
        BrickColorV("Light blue", 1102, Color3.fromRGB(128, 187, 219)),
        BrickColorV("Medium blue", 102, Color3.fromRGB(110, 153, 202)),
        BrickColorV("Dark green", 28, Color3.fromRGB(40, 127, 71)),
        BrickColorV("Lime green", 119, Color3.fromRGB(164, 189, 71)),
        BrickColorV("Really black", 1003, Color3.fromRGB(17, 17, 17))
    )

    val table: List<BrickColorV> get() = TABLE

    fun byName(name: String): BrickColorV? =
        TABLE.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun name(value: Int): String? = TABLE.firstOrNull { it.number == value }?.name

    fun color(value: Int): Color3? = TABLE.firstOrNull { it.number == value }?.color

    fun closest(color: Color3): BrickColorV {
        var best = TABLE.first()
        var bestDistance = Float.MAX_VALUE
        for (entry in TABLE) {
            val dr = entry.color.r - color.r
            val dg = entry.color.g - color.g
            val db = entry.color.b - color.b
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                best = entry
            }
        }
        return best
    }
}
