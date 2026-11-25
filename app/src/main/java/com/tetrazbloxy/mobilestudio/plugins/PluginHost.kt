package com.tetrazbloxy.mobilestudio.plugins

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.tetrazbloxy.mobilestudio.core.OutputLog
import com.tetrazbloxy.mobilestudio.core.PluginKind
import com.tetrazbloxy.mobilestudio.core.PluginManager
import com.tetrazbloxy.mobilestudio.format.RbxXmlCodec
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Background plugin loader. It mirrors the observable parts of SPEC 7.3:
 * exclusive activation, loading generation guard, script discovery, the
 * "no Scripts to run" log line, and the four second activation delay.
 *
 * The Lua bytecode interpreter itself is not part of this build, so script
 * sources are discovered and reported instead of executed.
 */
class PluginHost(
    private val manager: PluginManager,
    private val enabledStore: KeyValueStore,
    private val output: OutputLog,
    private val scope: CoroutineScope
) {
    data class Entry(
        val file: File,
        val name: String,
        val kind: PluginKind?,
        val enabled: Boolean,
        val scriptCount: Int,
        val error: String? = null
    )

    val entries = mutableStateListOf<Entry>()
    var activePlugin by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    private var generation = 0
    private var loadJob: Job? = null

    fun refresh() {
        val files = manager.list()
        val result = mutableListOf<Entry>()
        for (file in files) {
            val name = file.nameWithoutExtension
            val bytes = try {
                file.readBytes()
            } catch (_: Throwable) {
                null
            }
            if (bytes == null) {
                result.add(
                    Entry(file, name, null, manager.isEnabled(file.name), 0, "unreadable file")
                )
                continue
            }
            val kind = try {
                manager.detectKind(bytes)
            } catch (exception: Throwable) {
                result.add(
                    Entry(
                        file,
                        name,
                        null,
                        manager.isEnabled(file.name),
                        0,
                        exception.message ?: "unknown format"
                    )
                )
                continue
            }
            var scripts = 0
            var error: String? = null
            if (kind == PluginKind.RBXMX) {
                try {
                    val document = RbxXmlCodec.decode(bytes.toString(Charsets.UTF_8))
                    scripts = countScripts(document)
                } catch (exception: Throwable) {
                    error = "unreadable model"
                }
            } else if (kind == PluginKind.LUA) {
                scripts = 1
            } else if (kind == PluginKind.RBXM) {
                error = "binary plugin body not decoded"
            }
            result.add(
                Entry(file, name, kind, manager.isEnabled(file.name), scripts, error)
            )
        }
        entries.clear()
        entries.addAll(result)
    }

    fun loadAll() {
        val current = ++generation
        loadJob?.cancel()
        refresh()
        loading = true
        output.info("Loading ${entries.size} plugin(s)…")

        loadJob = scope.launch {
            activePlugin = null
            for (entry in entries) {
                if (generation != current) return@launch
                if (!entry.enabled) continue
                if (entry.error != null) {
                    output.warn("Failed to load plugin ${entry.name}: ${entry.error}")
                    continue
                }
                if (entry.kind == PluginKind.LUA) {
                    output.info("Loaded plugin ${entry.name} (1 script). Lua execution is not enabled in this build.")
                } else if (entry.scriptCount == 0) {
                    output.warn("Plugin ${entry.name} has no Scripts to run")
                } else {
                    output.info("Loaded plugin ${entry.name} (${entry.scriptCount} script(s)).")
                }
                delay(4_000)
                if (generation != current) return@launch
                activePlugin = entry.name
                output.info("Activated plugin ${entry.name}")
            }
            if (generation == current) {
                loading = false
                output.info("Plugins ready.")
            }
        }
    }

    fun install(bytes: ByteArray, fileName: String): Boolean = try {
        manager.install(bytes, fileName)
        refresh()
        loadAll()
        true
    } catch (exception: Throwable) {
        output.error("Install failed: ${exception.message}")
        false
    }

    fun installFromRoblox(assetId: Long, fetcher: (Long) -> ByteArray?): Boolean {
        val bytes = fetcher(assetId)
        if (bytes == null || bytes.isEmpty()) {
            output.error("Could not download plugin $assetId")
            return false
        }
        return install(bytes, "plugin-$assetId")
    }

    fun setEnabled(entry: Entry, enabled: Boolean) {
        manager.setEnabled(entry.file.name, enabled)
        refresh()
    }

    fun remove(entry: Entry) {
        manager.remove(entry.file.name)
        enabledStore.remove("off:${entry.file.name}")
        refresh()
        loadAll()
    }

    fun deactivate() {
        activePlugin = null
    }

    private fun countScripts(root: Instance): Int {
        var total = 0
        for (node in listOf(root) + root.getDescendants()) {
            if (node is Script) {
                val runContext = node.runContext.value
                val runnable = !node.disabled && node.enabled && runContext in 0..3
                if (runnable) total++
            }
        }
        return total
    }

    fun shutdown() {
        generation++
        loadJob?.cancel()
    }

    suspend fun installAsync(bytes: ByteArray, fileName: String): Boolean =
        withContext(Dispatchers.IO) { install(bytes, fileName) }
}
