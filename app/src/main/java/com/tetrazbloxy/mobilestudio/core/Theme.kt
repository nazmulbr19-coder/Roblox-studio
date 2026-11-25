package com.tetrazbloxy.mobilestudio.core

import org.json.JSONObject

/** UI theme: shared palette plus the four built-in themes. */
data class Theme(
    val name: String,
    val custom: Boolean,
    val accent: Int,
    val bg: Int,
    val border: Int,
    val card: Int,
    val dim: Int,
    val editorBg: Int,
    val editorGutter: Int,
    val editorText: Int,
    val header: Int,
    val hover: Int,
    val input: Int,
    val light: Int,
    val panel: Int,
    val ribbon: Int,
    val rowAlt: Int,
    val select: Int,
    val text: Int,
    val topbar: Int,
    val error: Int,
    val info: Int,
    val warn: Int,
    val play: Int,
    val stop: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("custom", custom)
        put("accent", accent)
        put("bg", bg)
        put("border", border)
        put("card", card)
        put("dim", dim)
        put("editorBg", editorBg)
        put("editorGutter", editorGutter)
        put("editorText", editorText)
        put("header", header)
        put("hover", hover)
        put("input", input)
        put("light", light)
        put("panel", panel)
        put("ribbon", ribbon)
        put("rowAlt", rowAlt)
        put("select", select)
        put("text", text)
        put("topbar", topbar)
        put("error", error)
        put("info", info)
        put("warn", warn)
        put("play", play)
        put("stop", stop)
    }

    companion object {
        val DARK = Theme(
            name = "Dark",
            custom = false,
            accent = 0xFF8B5CF6.toInt(),
            bg = 0xFF191919.toInt(),
            border = 0xFF2E2E2E.toInt(),
            card = 0xFF232323.toInt(),
            dim = 0xFF6B6B6B.toInt(),
            editorBg = 0xFF151515.toInt(),
            editorGutter = 0xFF1E1E1E.toInt(),
            editorText = 0xFFD4D4D4.toInt(),
            header = 0xFF202020.toInt(),
            hover = 0xFF2A2A2A.toInt(),
            input = 0xFF151515.toInt(),
            light = 0xFFE8E8E8.toInt(),
            panel = 0xFF1F1F1F.toInt(),
            ribbon = 0xFF242424.toInt(),
            rowAlt = 0xFF212121.toInt(),
            select = 0xFF3A2F55.toInt(),
            text = 0xFFE8E8E8.toInt(),
            topbar = 0xFF232323.toInt(),
            error = 0xFFFF5555.toInt(),
            info = 0xFF60A5FA.toInt(),
            warn = 0xFFFFAA00.toInt(),
            play = 0xFF2ECC71.toInt(),
            stop = 0xFFE74C3C.toInt()
        )

        val LIGHT = Theme(
            name = "Light",
            custom = false,
            accent = 0xFF6D28D9.toInt(),
            bg = 0xFFF4F4F5.toInt(),
            border = 0xFFD4D4D8.toInt(),
            card = 0xFFFFFFFF.toInt(),
            dim = 0xFF71717A.toInt(),
            editorBg = 0xFFFFFFFF.toInt(),
            editorGutter = 0xFFF4F4F5.toInt(),
            editorText = 0xFF27272A.toInt(),
            header = 0xFFEDEDF0.toInt(),
            hover = 0xFFE4E4E7.toInt(),
            input = 0xFFFFFFFF.toInt(),
            light = 0xFF18181B.toInt(),
            panel = 0xFFFAFAFA.toInt(),
            ribbon = 0xFFEFEFF1.toInt(),
            rowAlt = 0xFFF7F7F8.toInt(),
            select = 0xFFDDD6FE.toInt(),
            text = 0xFF18181B.toInt(),
            topbar = 0xFFEDEDF0.toInt(),
            error = 0xFFDC2626.toInt(),
            info = 0xFF2563EB.toInt(),
            warn = 0xFFD97706.toInt(),
            play = 0xFF16A34A.toInt(),
            stop = 0xFFDC2626.toInt()
        )

        val MIDNIGHT = Theme(
            name = "Midnight",
            custom = false,
            accent = 0xFF38BDF8.toInt(),
            bg = 0xFF0B1220.toInt(),
            border = 0xFF1E293B.toInt(),
            card = 0xFF111A2E.toInt(),
            dim = 0xFF64748B.toInt(),
            editorBg = 0xFF070D18.toInt(),
            editorGutter = 0xFF0D1524.toInt(),
            editorText = 0xFFCBD5E1.toInt(),
            header = 0xFF101B31.toInt(),
            hover = 0xFF17233A.toInt(),
            input = 0xFF0A1220.toInt(),
            light = 0xFFE2E8F0.toInt(),
            panel = 0xFF0E1728.toInt(),
            ribbon = 0xFF12203A.toInt(),
            rowAlt = 0xFF0D1626.toInt(),
            select = 0xFF16324F.toInt(),
            text = 0xFFE2E8F0.toInt(),
            topbar = 0xFF101B31.toInt(),
            error = 0xFFF87171.toInt(),
            info = 0xFF38BDF8.toInt(),
            warn = 0xFFFBBF24.toInt(),
            play = 0xFF34D399.toInt(),
            stop = 0xFFF87171.toInt()
        )

        val HIGH_CONTRAST = Theme(
            name = "High Contrast",
            custom = false,
            accent = 0xFFFFD400.toInt(),
            bg = 0xFF000000.toInt(),
            border = 0xFFFFFFFF.toInt(),
            card = 0xFF0A0A0A.toInt(),
            dim = 0xFFCCCCCC.toInt(),
            editorBg = 0xFF000000.toInt(),
            editorGutter = 0xFF111111.toInt(),
            editorText = 0xFFFFFFFF.toInt(),
            header = 0xFF111111.toInt(),
            hover = 0xFF333333.toInt(),
            input = 0xFF000000.toInt(),
            light = 0xFFFFFFFF.toInt(),
            panel = 0xFF0A0A0A.toInt(),
            ribbon = 0xFF111111.toInt(),
            rowAlt = 0xFF0F0F0F.toInt(),
            select = 0xFF404000.toInt(),
            text = 0xFFFFFFFF.toInt(),
            topbar = 0xFF111111.toInt(),
            error = 0xFFFF4444.toInt(),
            info = 0xFF00FFFF.toInt(),
            warn = 0xFFFFFF00.toInt(),
            play = 0xFF00FF66.toInt(),
            stop = 0xFFFF4444.toInt()
        )

        val ALL: List<Theme> = listOf(DARK, LIGHT, MIDNIGHT, HIGH_CONTRAST)

        fun byName(name: String): Theme? = ALL.firstOrNull { it.name == name }

        fun fromJson(json: JSONObject): Theme = Theme(
            name = json.optString("name", "Custom"),
            custom = json.optBoolean("custom", true),
            accent = json.optInt("accent", DARK.accent),
            bg = json.optInt("bg", DARK.bg),
            border = json.optInt("border", DARK.border),
            card = json.optInt("card", DARK.card),
            dim = json.optInt("dim", DARK.dim),
            editorBg = json.optInt("editorBg", DARK.editorBg),
            editorGutter = json.optInt("editorGutter", DARK.editorGutter),
            editorText = json.optInt("editorText", DARK.editorText),
            header = json.optInt("header", DARK.header),
            hover = json.optInt("hover", DARK.hover),
            input = json.optInt("input", DARK.input),
            light = json.optInt("light", DARK.light),
            panel = json.optInt("panel", DARK.panel),
            ribbon = json.optInt("ribbon", DARK.ribbon),
            rowAlt = json.optInt("rowAlt", DARK.rowAlt),
            select = json.optInt("select", DARK.select),
            text = json.optInt("text", DARK.text),
            topbar = json.optInt("topbar", DARK.topbar),
            error = json.optInt("error", DARK.error),
            info = json.optInt("info", DARK.info),
            warn = json.optInt("warn", DARK.warn),
            play = json.optInt("play", DARK.play),
            stop = json.optInt("stop", DARK.stop)
        )
    }
}
