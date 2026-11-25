package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArrayList

/**
 * All UI/preference keys persisted in the `studio_settings` SharedPreferences file.
 * Keys and defaults follow SPEC section 7.1 exactly.
 *
 * Quirk preserved from the original: only [modernMaterials] notifies listeners;
 * the other setters write silently.
 */
class Settings(private val store: KeyValueStore) {
    companion object {
        const val PREFS = "studio_settings"
        private const val KEY_CUSTOM_THEMES = "customThemes"
    }

    val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    var autoCloseBrackets: Boolean
        get() = store.getBoolean("autoClose", true)
        set(value) = store.putBoolean("autoClose", value)

    var autoIndent: Boolean
        get() = store.getBoolean("autoIndent", true)
        set(value) = store.putBoolean("autoIndent", value)

    var autocomplete: Boolean
        get() = store.getBoolean("autocomplete", true)
        set(value) = store.putBoolean("autocomplete", value)

    var cameraMode: String
        get() = store.getString("cameraMode", "Follow") ?: "Follow"
        set(value) = store.putString("cameraMode", value)

    var cameraSensitivity: Float
        get() = store.getFloat("camSens", 1.0f)
        set(value) = store.putFloat("camSens", value)

    var cameraSmoothing: Boolean
        get() = store.getBoolean("camSmooth", true)
        set(value) = store.putBoolean("camSmooth", value)

    var confirmDelete: Boolean
        get() = store.getBoolean("confirmDel", false)
        set(value) = store.putBoolean("confirmDel", value)

    var dragCollisions: Boolean
        get() = store.getBoolean("dragCollisions", false)
        set(value) = store.putBoolean("dragCollisions", value)

    var editorFontSize: Float
        get() = store.getFloat("editorFont", 13.0f)
        set(value) = store.putFloat("editorFont", value)

    var flySpeed: Float
        get() = store.getFloat("flySpeed", 1.0f)
        set(value) = store.putFloat("flySpeed", value)

    var graphicsQuality: Int
        get() = store.getInt("gfx", 10)
        set(value) = store.putInt("gfx", value)

    var guiGrid: Int
        get() = store.getInt("guiGrid", 0)
        set(value) = store.putInt("guiGrid", value)

    var guiSmartGuides: Boolean
        get() = store.getBoolean("guiGuides", true)
        set(value) = store.putBoolean("guiGuides", value)

    var hideWindowsInPlaytest: Boolean
        get() = store.getBoolean("hidePlay", false)
        set(value) = store.putBoolean("hidePlay", value)

    /** The only ordinary setter that notifies listeners (SPEC 7.1). */
    var modernMaterials: Boolean
        get() = store.getBoolean("modernMaterials", true)
        set(value) {
            store.putBoolean("modernMaterials", value)
            changed("modernMaterials")
        }

    var moveSnap: Float
        get() = store.getFloat("moveSnap", 1.0f)
        set(value) = store.putFloat("moveSnap", value)

    var moveSnapOn: Boolean
        get() = store.getBoolean("moveSnapOn", true)
        set(value) = store.putBoolean("moveSnapOn", value)

    var movementMode: String
        get() = store.getString("movementMode", "Dynamic Thumbstick") ?: "Dynamic Thumbstick"
        set(value) = store.putString("movementMode", value)

    var playCameraSensitivity: Float
        get() = store.getFloat("playSens", 1.0f)
        set(value) = store.putFloat("playSens", value)

    var pluginUiScale: Float
        get() = store.getFloat("pluginScale", 0.0f)
        set(value) = store.putFloat("pluginScale", value)

    var renderDistance: String
        get() = store.getString("renderDist", "Automatic") ?: "Automatic"
        set(value) = store.putString("renderDist", value)

    var rotSnap: Float
        get() = store.getFloat("rotSnap", 15.0f)
        set(value) = store.putFloat("rotSnap", value)

    var rotSnapOn: Boolean
        get() = store.getBoolean("rotSnapOn", true)
        set(value) = store.putBoolean("rotSnapOn", value)

    var selectInsideModels: Boolean
        get() = store.getBoolean("selInside", false)
        set(value) = store.putBoolean("selInside", value)

    var shiftLockSwitch: Boolean
        get() = store.getBoolean("shiftLockSwitch", true)
        set(value) = store.putBoolean("shiftLockSwitch", value)

    var showFps: Boolean
        get() = store.getBoolean("showFps", true)
        set(value) = store.putBoolean("showFps", value)

    var themeName: String
        get() = store.getString("theme", "Dark") ?: "Dark"
        set(value) = store.putString("theme", value)

    var volume: Int
        get() = store.getInt("volume", 10)
        set(value) = store.putInt("volume", value)

    /** Master snap flag derived from the two independent toggles. */
    val snapEnabled: Boolean
        get() = moveSnapOn || rotSnapOn

    fun getTheme(): Theme {
        val requested = themeName
        return (Theme.ALL + customThemes()).firstOrNull { it.name == requested } ?: Theme.DARK
    }

    fun customThemes(): List<Theme> {
        val raw = store.getString(KEY_CUSTOM_THEMES) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<Theme>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                result.add(Theme.fromJson(item))
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveCustomThemes(themes: List<Theme>) {
        val array = JSONArray()
        for (theme in themes) {
            array.put(theme.toJson())
        }
        store.putString(KEY_CUSTOM_THEMES, array.toString())
    }

    fun addListener(listener: (String) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    /** Notifies every listener. Only [modernMaterials] calls this automatically. */
    fun changed(key: String = "settings") {
        for (listener in listeners) {
            listener(key)
        }
    }
}
