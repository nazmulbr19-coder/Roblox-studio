package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    private fun newSettings(store: InMemoryKeyValueStore = InMemoryKeyValueStore()) =
        Settings(store) to store

    @Test
    fun defaultsMatchSpec() {
        val (settings, _) = newSettings()

        assertTrue(settings.autoCloseBrackets)
        assertTrue(settings.autoIndent)
        assertTrue(settings.autocomplete)
        assertEquals("Follow", settings.cameraMode)
        assertEquals(1.0f, settings.cameraSensitivity, 1e-6f)
        assertTrue(settings.cameraSmoothing)
        assertFalse(settings.confirmDelete)
        assertFalse(settings.dragCollisions)
        assertEquals(13.0f, settings.editorFontSize, 1e-6f)
        assertEquals(1.0f, settings.flySpeed, 1e-6f)
        assertEquals(10, settings.graphicsQuality)
        assertEquals(0, settings.guiGrid)
        assertTrue(settings.guiSmartGuides)
        assertFalse(settings.hideWindowsInPlaytest)
        assertTrue(settings.modernMaterials)
        assertEquals(1.0f, settings.moveSnap, 1e-6f)
        assertTrue(settings.moveSnapOn)
        assertEquals("Dynamic Thumbstick", settings.movementMode)
        assertEquals(1.0f, settings.playCameraSensitivity, 1e-6f)
        assertEquals(0.0f, settings.pluginUiScale, 1e-6f)
        assertEquals("Automatic", settings.renderDistance)
        assertEquals(15.0f, settings.rotSnap, 1e-6f)
        assertTrue(settings.rotSnapOn)
        assertFalse(settings.selectInsideModels)
        assertTrue(settings.shiftLockSwitch)
        assertTrue(settings.showFps)
        assertEquals("Dark", settings.themeName)
        assertEquals(10, settings.volume)
    }

    @Test
    fun valuesPersistAcrossInstances() {
        val (settings, store) = newSettings()
        settings.flySpeed = 3.5f
        settings.volume = 4
        settings.renderDistance = "Far"
        settings.themeName = "Midnight"

        val reloaded = Settings(store)
        assertEquals(3.5f, reloaded.flySpeed, 1e-6f)
        assertEquals(4, reloaded.volume)
        assertEquals("Far", reloaded.renderDistance)
        assertEquals("Midnight", reloaded.themeName)
    }

    @Test
    fun onlyModernMaterialsNotifiesListeners() {
        val (settings, _) = newSettings()
        var notifications = 0
        settings.addListener { notifications++ }

        settings.flySpeed = 2f
        assertEquals(0, notifications)

        settings.modernMaterials = false
        assertEquals(1, notifications)

        settings.changed()
        assertEquals(2, notifications)
    }

    @Test
    fun themeFallsBackToDarkAndResolvesBuiltIns() {
        val (settings, _) = newSettings()
        assertEquals(Theme.DARK, settings.getTheme())

        settings.themeName = "Light"
        assertEquals(Theme.LIGHT, settings.getTheme())

        settings.themeName = "Does Not Exist"
        assertEquals(Theme.DARK, settings.getTheme())
    }

    @Test
    fun customThemesRoundTrip() {
        val (settings, store) = newSettings()
        val custom = Theme.DARK.copy(name = "My Theme", custom = true, accent = 0x123456)
        settings.saveCustomThemes(listOf(custom))

        val reloaded = Settings(store)
        assertEquals(1, reloaded.customThemes().size)
        assertEquals("My Theme", reloaded.customThemes().first().name)

        reloaded.themeName = "My Theme"
        assertEquals(custom, reloaded.getTheme())
    }

    @Test
    fun malformedCustomThemesYieldEmptyList() {
        val store = InMemoryKeyValueStore()
        store.putString("customThemes", "{not valid json")
        val settings = Settings(store)
        assertTrue(settings.customThemes().isEmpty())
    }

    @Test
    fun snapEnabledIsDerived() {
        val (settings, _) = newSettings()
        assertTrue(settings.snapEnabled)
        settings.moveSnapOn = false
        assertTrue(settings.snapEnabled)
        settings.rotSnapOn = false
        assertFalse(settings.snapEnabled)
    }
}
