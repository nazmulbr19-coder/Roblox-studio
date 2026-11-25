package com.tetrazbloxy.mobilestudio.studio

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tetrazbloxy.mobilestudio.audio.AudioEngine
import com.tetrazbloxy.mobilestudio.core.DockLayoutState
import com.tetrazbloxy.mobilestudio.core.OutputLog
import com.tetrazbloxy.mobilestudio.core.PluginManager
import com.tetrazbloxy.mobilestudio.core.Recents
import com.tetrazbloxy.mobilestudio.core.Settings
import com.tetrazbloxy.mobilestudio.core.Theme
import com.tetrazbloxy.mobilestudio.core.UndoManager
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.model.Vector3
import com.tetrazbloxy.mobilestudio.net.Accounts
import com.tetrazbloxy.mobilestudio.net.AssetFetcher
import com.tetrazbloxy.mobilestudio.net.ImageLoader
import com.tetrazbloxy.mobilestudio.net.RobloxCloud
import com.tetrazbloxy.mobilestudio.net.WebPermissions
import com.tetrazbloxy.mobilestudio.playtest.AvatarKind
import com.tetrazbloxy.mobilestudio.playtest.PlaytestSession
import com.tetrazbloxy.mobilestudio.plugins.PluginHost
import kotlinx.coroutines.CoroutineScope

enum class AppScreen { START, STUDIO }

enum class DockTab(val title: String) {
    EXPLORER("Explorer"),
    PROPERTIES("Properties"),
    TOOLBOX("Toolbox"),
    OUTPUT("Output"),
    SCRIPTS("Scripts")
}

enum class AppSheet {
    FILE_MENU,
    SETTINGS,
    ACCOUNT,
    PLUGINS,
    ABOUT,
    AVATAR,
    AUDIO
}

data class ToastEvent(val id: Long, val text: String)

sealed interface AppDialog {
    data class Message(
        val title: String,
        val message: String,
        val onDismiss: () -> Unit = {}
    ) : AppDialog

    data class Confirm(
        val title: String,
        val message: String,
        val positive: String = "OK",
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit = {}
    ) : AppDialog

    data class Prompt(
        val title: String,
        val label: String,
        val initial: String = "",
        val onResult: (String?) -> Unit
    ) : AppDialog

    data class Number(
        val title: String,
        val label: String,
        val initial: Float = 0f,
        val onResult: (Float?) -> Unit
    ) : AppDialog

    data class ColorPick(
        val title: String,
        val initial: Color3,
        val onResult: (Color3) -> Unit
    ) : AppDialog

    data class PickClass(val onResult: (String?) -> Unit) : AppDialog

    data class WebPermission(val host: String, val who: String?) : AppDialog

    data class Unsaved(
        val onSave: () -> Unit,
        val onDiscard: () -> Unit
    ) : AppDialog

    data class Recovery(
        val name: String,
        val time: Long,
        val onRecover: () -> Unit,
        val onDiscard: () -> Unit
    ) : AppDialog
}

/**
 * Single source of truth for the single-activity app. All persistence classes
 * come from the core layer; this class adds Compose-observable UI state and
 * hooks for pickers/dialogs that can only be created by the activity.
 */
class StudioSession(
    val app: Application,
    val settings: Settings,
    val recents: Recents,
    val dock: DockLayoutState,
    val output: OutputLog,
    val plugins: PluginManager,
    val pluginHost: PluginHost,
    val accounts: Accounts,
    val cloud: RobloxCloud,
    val webPermissions: WebPermissions,
    val assets: AssetFetcher,
    val images: ImageLoader,
    val audio: AudioEngine,
    val scope: CoroutineScope
) {
    var screen by mutableStateOf(AppScreen.START)
    var document by mutableStateOf(PlaceFactory.newPlace(PlaceTemplate.BASEPLATE))
    var documentName by mutableStateOf("Untitled Place")
    var fileUri by mutableStateOf<Uri?>(null)
    var fileIsXml by mutableStateOf(true)
    var dirty by mutableStateOf(false)
    var selection by mutableStateOf<List<Instance>>(emptyList())
    val openScripts = mutableStateListOf<Script>()
    var activeScript by mutableStateOf<Script?>(null)
    var showEditor by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var play by mutableStateOf<PlaytestSession?>(null)
    var avatarKind by mutableStateOf(AvatarKind.R15)
    var morphToPlayerChoice by mutableStateOf(false)
    var cloudPlace by mutableStateOf<RobloxCloud.CloudGame?>(null)

    var dialog by mutableStateOf<AppDialog?>(null)
    var sheet by mutableStateOf<AppSheet?>(null)
    var dockTab by mutableStateOf(DockTab.EXPLORER)
    var dockVersion by mutableStateOf(0)
    var toast by mutableStateOf<ToastEvent?>(null)
    var sceneVersion by mutableStateOf(0)
    var theme by mutableStateOf(settings.getTheme())
    var themeVersion by mutableStateOf(0)
    var lastAutosave by mutableStateOf(0L)

    var camTarget by mutableStateOf(Vector3(0f, 4f, 0f))
    var camYaw by mutableStateOf(-0.55f)
    var camPitch by mutableStateOf(-0.32f)
    var camDistance by mutableStateOf(46f)
    var showGrid by mutableStateOf(true)
    var showWireframe by mutableStateOf(false)

    val undo = UndoManager()
    var clipboard: List<Instance> = emptyList()
    var library: ModelLibrary = ModelLibrary(app)
    val startCache: StartCache = StartCache(
        com.tetrazbloxy.mobilestudio.storage.SharedPrefsKeyValueStore(app, StartCache.PREFS)
    )

    // Hooks installed by the Compose tree because activity-result launchers
    // must be remembered inside composition.
    var requestOpenFile: (() -> Unit)? = null
    var requestSave: (() -> Unit)? = null
    var requestSaveAs: ((String, (Uri?) -> Unit) -> Unit)? = null
    var requestOpenPlugin: (() -> Unit)? = null
    var requestImportModel: (() -> Unit)? = null
    var requestExport: ((String, (Uri?) -> Unit) -> Unit)? = null
    var afterSave: (() -> Unit)? = null
    var pendingPickerResult: ((Uri?) -> Unit)? = null

    val autosave = AutosaveManager(app)

    init {
        settings.addListener {
            theme = settings.getTheme()
            themeVersion++
        }

        webPermissions.asker = { host, who ->
            Handler(Looper.getMainLooper()).post {
                dialog = AppDialog.WebPermission(host, who)
            }
        }

        autosave.start(
            scope = scope,
            snapshot = { DocumentCodec.encode(document, documentName) },
            isDirty = { dirty },
            onSaved = { lastAutosave = it }
        )
    }

    val isPhone: Boolean get() = dock.phone

    val documentTitle: String
        get() = if (dirty) "$documentName •" else documentName

    val selectedPart: com.tetrazbloxy.mobilestudio.model.BasePart?
        get() = selection.firstOrNull() as? com.tetrazbloxy.mobilestudio.model.BasePart

    val selectionLabel: String
        get() = when (selection.size) {
            0 -> "Nothing selected"
            1 -> selection.first().name
            else -> "${selection.size} selected"
        }

    fun notify(text: String) {
        toast = ToastEvent(System.nanoTime(), text)
        output.info(text)
    }

    fun dismissToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun touchScene() {
        sceneVersion++
    }

    fun markDirty() {
        dirty = true
        touchScene()
    }

    fun markSaved() {
        dirty = false
        afterSave?.let {
            afterSave = null
            it()
        }
    }

    fun applyTheme(name: String) {
        settings.themeName = name
        settings.changed("theme")
        theme = settings.getTheme()
        themeVersion++
    }

    fun setSelection(instances: List<Instance>) {
        selection = instances
    }

    fun toggleDock(id: String) {
        dock.toggle(id)
        dockVersion++
    }

    fun setDocksHidden(hidden: Boolean) {
        dock.setAllHidden(hidden)
        dockVersion++
    }

    fun updateDockWidth(side: String, value: Int) {
        when (side) {
            "left" -> dock.leftW = value.coerceIn(120, 620)
            "right" -> dock.rightW = value.coerceIn(120, 620)
            else -> dock.bottomH = value.coerceIn(70, 520)
        }
        dockVersion++
    }

    fun persistDock() {
        dock.save()
    }

    fun toggleSelection(instance: Instance) {
        selection = if (selection.any { it === instance }) {
            selection.filterNot { it === instance }
        } else {
            selection + instance
        }
    }

    fun showStudio() {
        screen = AppScreen.STUDIO
        sheet = null
        dialog = null
    }

    fun showStart() {
        screen = AppScreen.START
    }

    fun focusCamera(position: Vector3, distance: Float = 28f) {
        camTarget = position
        camDistance = distance.coerceIn(2f, 400f)
    }

    fun checkRecovery() {
        val recovery = autosave.recovery() ?: return
        if (dirty) return
        dialog = AppDialog.Recovery(
            name = recovery.name,
            time = recovery.time,
            onRecover = {
                dialog = null
                try {
                    document = DocumentCodec.decode(recovery.xml.toByteArray())
                    documentName = recovery.name
                    fileUri = recovery.uri?.let { Uri.parse(it) }
                    dirty = true
                    selection = emptyList()
                    openScripts.clear()
                    activeScript = null
                    showEditor = false
                    undo.clear()
                    touchScene()
                    screen = AppScreen.STUDIO
                    notify("Recovered ${recovery.name}")
                } catch (exception: Throwable) {
                    dialog = AppDialog.Message(
                        "Recovery failed",
                        exception.message ?: "The autosave could not be decoded."
                    )
                }
            },
            onDiscard = {
                dialog = null
                autosave.clear()
            }
        )
    }

    fun dismissDialog() {
        val current = dialog
        if (current is AppDialog.WebPermission) {
            webPermissions.answer(current.host, null)
        }
        dialog = null
    }
}
