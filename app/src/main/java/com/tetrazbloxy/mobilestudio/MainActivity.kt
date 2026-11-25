package com.tetrazbloxy.mobilestudio

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tetrazbloxy.mobilestudio.playtest.HudPage
import com.tetrazbloxy.mobilestudio.studio.AppScreen
import com.tetrazbloxy.mobilestudio.ui.StudioRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var app: StudioApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as StudioApp
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        setContent {
            StudioRoot(app = app, onExit = { finish() })
        }

        onBackPressedDispatcher.addCallback(this) {
            val session = app.session
            when {
                session.dialog != null -> session.dismissDialog()
                session.sheet != null -> session.sheet = null
                session.playing && session.play?.hudPage != HudPage.NONE -> {
                    session.play?.hidePage()
                }
                session.play?.chatOpen == true -> session.play?.closeChat()
                session.showEditor -> session.showEditor = false
                session.screen == AppScreen.STUDIO -> session.showStart()
                else -> finish()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        if (isFinishing) {
            app.shutdown()
        }
        super.onDestroy()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        app.session.scope.launch {
            app.controller.openUri(uri)
        }
    }
}
