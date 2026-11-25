package com.tetrazbloxy.mobilestudio.studio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * In-process autosave loop. Every 30 seconds a dirty document is encoded and
 * written atomically to `filesDir/autosave`, with a small metadata sidecar so
 * the next launch can offer recovery.
 */
class AutosaveManager(context: Context) {
    data class Recovery(
        val name: String,
        val uri: String?,
        val time: Long,
        val xml: String
    )

    private val dir = File(context.filesDir, "autosave").apply { mkdirs() }
    private val documentFile = File(dir, "current.rbxmx")
    private val metaFile = File(dir, "current.json")
    private var job: Job? = null

    @Volatile
    var lastSavedAt: Long = 0L
        private set

    fun start(
        scope: CoroutineScope,
        snapshot: () -> String?,
        isDirty: () -> Boolean,
        onSaved: (Long) -> Unit = {}
    ) {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(30_000)
                if (!isDirty()) continue
                val xml = snapshot() ?: continue
                save(xml, null, "Autosave")
                onSaved(lastSavedAt)
            }
        }
    }

    suspend fun saveNow(xml: String, uri: String?, name: String): Long {
        save(xml, uri, name)
        return lastSavedAt
    }

    private suspend fun save(xml: String, uri: String?, name: String) {
        withContext(Dispatchers.IO) {
            try {
                dir.mkdirs()
                val tmp = File(dir, "current.tmp")
                tmp.writeText(xml)
                if (!tmp.renameTo(documentFile)) {
                    documentFile.writeText(xml)
                    tmp.delete()
                }
                val meta = JSONObject().apply {
                    put("name", name)
                    put("uri", uri ?: "")
                    put("time", System.currentTimeMillis())
                }
                metaFile.writeText(meta.toString())
                lastSavedAt = System.currentTimeMillis()
            } catch (_: Throwable) {
                // Autosave must never interrupt editing.
            }
        }
    }

    fun recovery(): Recovery? {
        if (!documentFile.isFile || documentFile.length() == 0L) return null
        val xml = try {
            documentFile.readText()
        } catch (_: Throwable) {
            return null
        }
        val meta = try {
            JSONObject(metaFile.readText())
        } catch (_: Throwable) {
            null
        }
        return Recovery(
            name = meta?.optString("name", "Recovered Place") ?: "Recovered Place",
            uri = meta?.optString("uri", "")?.takeIf { it.isNotBlank() },
            time = meta?.optLong("time", documentFile.lastModified())
                ?: documentFile.lastModified(),
            xml = xml
        )
    }

    fun clear() {
        documentFile.delete()
        metaFile.delete()
        lastSavedAt = 0L
    }

    fun stop() {
        job?.cancel()
    }
}
