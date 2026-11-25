package com.tetrazbloxy.mobilestudio.model

/** Keyframe for NumberSequence property values. */
data class NumberKey(val value: Float, val envelope: Float, val time: Float)

/** Keyframe for ColorSequence property values. */
data class ColorKey(val color: Color3, val envelope: Float, val time: Float)

/** Piecewise color ramp (Roblox ColorSequence). */
data class ColorSequence(val keypoints: List<ColorKey>) {
    init {
        require(keypoints.isNotEmpty()) { "ColorSequence requires at least one keypoint" }
    }

    companion object {
        fun new(color: Color3): ColorSequence = ColorSequence(listOf(ColorKey(color, 0f, 0f)))

        fun new(first: Color3, second: Color3): ColorSequence = ColorSequence(
            listOf(
                ColorKey(first, 0f, 0f),
                ColorKey(second, 0f, 1f)
            )
        )
    }
}

/** Piecewise numeric ramp (Roblox NumberSequence). */
data class NumberSequence(val keypoints: List<NumberKey>) {
    init {
        require(keypoints.isNotEmpty()) { "NumberSequence requires at least one keypoint" }
    }

    companion object {
        fun new(value: Float): NumberSequence = NumberSequence(listOf(NumberKey(value, 0f, 0f)))

        fun new(first: Float, second: Float): NumberSequence = NumberSequence(
            listOf(
                NumberKey(first, 0f, 0f),
                NumberKey(second, 0f, 1f)
            )
        )
    }
}
