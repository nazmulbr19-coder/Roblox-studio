package com.tetrazbloxy.mobilestudio

import android.app.Application
import android.util.Log
import com.tetrazbloxy.mobilestudio.audio.AudioEngine
import com.tetrazbloxy.mobilestudio.core.DockLayoutState
import com.tetrazbloxy.mobilestudio.core.OutputLog
import com.tetrazbloxy.mobilestudio.core.PluginManager
import com.tetrazbloxy.mobilestudio.core.Recents
import com.tetrazbloxy.mobilestudio.core.Settings
import com.tetrazbloxy.mobilestudio.net.Accounts
import com.tetrazbloxy.mobilestudio.net.AssetFetcher
import com.tetrazbloxy.mobilestudio.net.ImageLoader
import com.tetrazbloxy.mobilestudio.net.RobloxCloud
import com.tetrazbloxy.mobilestudio.net.WebPermissions
import com.tetrazbloxy.mobilestudio.plugins.PluginHost
import com.tetrazbloxy.mobilestudio.storage.SharedPrefsKeyValueStore
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Owns the single app-wide session graph. Everything except the Activity's
 * file pickers lives here so configuration changes and Activity re-creation
 * keep documents, undo history, plugins and cloud state intact.
 */
class StudioApp : Application() {
    lateinit var settings: Settings
        private set

    lateinit var recents: Recents
        private set

    lateinit var dockLayout: DockLayoutState
        private set

    lateinit var output: OutputLog
        private set

    lateinit var accounts: Accounts
        private set

    lateinit var cloud: RobloxCloud
        private set

    lateinit var webPermissions: WebPermissions
        private set

    lateinit var assets: AssetFetcher
        private set

    lateinit var images: ImageLoader
        private set

    lateinit var audio: AudioEngine
        private set

    lateinit var pluginManager: PluginManager
        private set

    lateinit var pluginHost: PluginHost
        private set

    lateinit var session: StudioSession
        private set

    lateinit var controller: StudioController
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        settings = Settings(SharedPrefsKeyValueStore(this, Settings.PREFS))
        recents = Recents(SharedPrefsKeyValueStore(this, Recents.PREFS))
        dockLayout = DockLayoutState(
            store = SharedPrefsKeyValueStore(this, LAYOUT_PREFS),
            phone = resources.configuration.smallestScreenWidthDp < 600
        )
        output = OutputLog()
        accounts = Accounts(SharedPrefsKeyValueStore(this, Accounts.PREFS))
        cloud = RobloxCloud(accounts)
        webPermissions = WebPermissions(SharedPrefsKeyValueStore(this, WebPermissions.PREFS))
        assets = AssetFetcher(this)
        images = ImageLoader(this)
        audio = AudioEngine().also {
            it.setMasterVolume(settings.volume.coerceIn(0, 10) / 10f)
        }
        pluginManager = PluginManager(
            dir = File(filesDir, "plugins"),
            prefs = SharedPrefsKeyValueStore(this, PLUGIN_PREFS),
            log = { output.info(it) }
        )
        pluginHost = PluginHost(
            manager = pluginManager,
            enabledStore = SharedPrefsKeyValueStore(this, PLUGIN_PREFS),
            output = output,
            scope = appScope
        )

        session = StudioSession(
            app = this,
            settings = settings,
            recents = recents,
            dock = dockLayout,
            output = output,
            plugins = pluginManager,
            pluginHost = pluginHost,
            accounts = accounts,
            cloud = cloud,
            webPermissions = webPermissions,
            assets = assets,
            images = images,
            audio = audio,
            scope = appScope
        )
        controller = StudioController(session)

        settings.addListener {
            audio.setMasterVolume(settings.volume.coerceIn(0, 10) / 10f)
        }

        if (!started) {
            started = true
            output.info("Mobile Studio 2.5 (${BuildConfig.VERSION_CODE}) ready")
            Log.i(TAG, "Mobile Studio ready. v${BuildConfig.VERSION_NAME}")
        }

        pluginHost.loadAll()
        appScope.launch {
            val recovery = session.autosave.recovery()
            if (recovery != null) {
                output.info("Recovery snapshot available: ${recovery.name}")
            }
        }
    }

    fun shutdown() {
        pluginHost.shutdown()
        audio.release()
        assets.shutdown()
    }

    companion object {
        const val TAG = "MobileStudio"
        const val LAYOUT_PREFS = "layout"
        const val PLUGIN_PREFS = "plugins"

        @Volatile
        var started: Boolean = false
    }
}
