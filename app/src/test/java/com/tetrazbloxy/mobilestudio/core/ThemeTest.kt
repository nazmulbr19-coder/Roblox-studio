package com.tetrazbloxy.mobilestudio.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test
    fun allBuiltInsExist() {
        assertEquals(4, Theme.ALL.size)
        assertEquals(Theme.DARK, Theme.byName("Dark"))
        assertEquals(Theme.LIGHT, Theme.byName("Light"))
        assertEquals(Theme.MIDNIGHT, Theme.byName("Midnight"))
        assertEquals(Theme.HIGH_CONTRAST, Theme.byName("High Contrast"))
        assertNull(Theme.byName("Nope"))
    }

    @Test
    fun jsonRoundTripPreservesEveryColor() {
        for (theme in Theme.ALL) {
            val restored = Theme.fromJson(theme.toJson())
            assertEquals(theme, restored)
        }
    }

    @Test
    fun customFlagSurvivesJson() {
        val custom = Theme.DARK.copy(name = "Custom", custom = true, accent = 0xFF00FF00.toInt())
        val restored = Theme.fromJson(custom.toJson())
        assertTrue(restored.custom)
        assertEquals("Custom", restored.name)
        assertEquals(0xFF00FF00.toInt(), restored.accent)
    }

    @Test
    fun missingJsonFieldsFallBackToDark() {
        val restored = Theme.fromJson(JSONObject())
        assertEquals("Custom", restored.name)
        assertEquals(Theme.DARK.accent, restored.accent)
        assertEquals(Theme.DARK.stop, restored.stop)
        assertTrue(restored.custom)
    }
}
