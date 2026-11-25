package com.tetrazbloxy.mobilestudio.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tetrazbloxy.mobilestudio.StudioApp
import com.tetrazbloxy.mobilestudio.studio.AppScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StudioRoot(app: StudioApp, onExit: () -> Unit) {
    val session = app.session
    val controller = app.controller
    val context = LocalContext.current

    var theme by remember(session) { mutableStateOf(session.theme) }
    LaunchedEffect(session.themeVersion) { theme = session.theme }
    LaunchedEffect(Unit) { session.checkRecovery() }

    StudioTheme(theme) {
        CompositionLocalProvider(LocalStudioSessionOwner provides session) {
            val openLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    session.scope.launch { controller.openUri(uri) }
                }
            }
            val createLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/xml")
            ) { uri ->
                session.pendingPickerResult?.invoke(uri)
                session.pendingPickerResult = null
            }
            val pluginLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                session.scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    if (bytes != null) {
                        controller.installPluginBytes(bytes, uri.lastPathSegment ?: "plugin")
                    }
                }
            }
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                session.scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    if (bytes != null) {
                        controller.importModelToLibrary(bytes, uri.lastPathSegment ?: "model")
                    }
                }
            }

            session.requestOpenFile = { openLauncher.launch(arrayOf("*/*")) }
            session.requestSaveAs = { suggested, onPicked ->
                session.pendingPickerResult = onPicked
                createLauncher.launch(suggested)
            }
            session.requestOpenPlugin = { pluginLauncher.launch(arrayOf("*/*")) }
            session.requestImportModel = { importLauncher.launch(arrayOf("*/*")) }
            session.requestExport = { suggested, onPicked ->
                session.pendingPickerResult = onPicked
                createLauncher.launch(suggested)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (session.screen) {
                    AppScreen.START -> StartScreen(session, controller)
                    AppScreen.STUDIO -> StudioScreen(session, controller)
                }

                session.dialog?.let { dialog -> DialogHost(session, controller, dialog) }
                session.sheet?.let { sheet -> SheetHost(session, controller, sheet) }
                ToastHost(session)
            }
        }
    }
}
