package com.tetrazbloxy.mobilestudio.net

import kotlin.math.pow

/** Asset-id and price helpers shared by the toolbox/cloud clients. */
object AssetIds {
    private val ID_PATTERN = Regex(
        """(?:[?&]id=|/asset/|/library/|/catalog/|/store/asset/|/plugins/|/models/|rbxassetid://)(\d+)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Accepts a plain numeric id or any of the URL forms listed in SPEC 9.3.
     * Returns null when no id can be extracted.
     */
    fun parseAssetId(input: String?): Long? {
        val text = input?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.all { it.isDigit() }) {
            return text.toLongOrNull()
        }
        return ID_PATTERN.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    /** Roblox price conversion: round(significand * 10^(exponent + 2)). */
    fun cents(significand: Double, exponent: Int): Long {
        if (significand == 0.0) return 0L
        return Math.round(significand * 10.0.pow(exponent + 2))
    }
}

/** Immutable cloud place triple returned by the game-listing APIs. */
data class CloudPlace(
    val name: String,
    val placeId: Long,
    val universeId: Long
)
