package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream

class PluginFormatException(message: String) : Exception(message)

enum class PluginKind(val extension: String) {
    RBXM("rbxm"),
    RBXMX("rbxmx"),
    LUA("lua")
}

/**
 * File/state half of `core.PluginManager` (SPEC 4.2 / 7.3).
 * Script execution is intentionally out of scope for this data/logic pass.
 */
class PluginManager(
    private val dir: File,
    private val prefs: KeyValueStore,
    private val log: (String) -> Unit = {}
) {
    companion object {
        private val KNOWN_EXTENSIONS = listOf(".rbxm", ".rbxmx", ".rbxl", ".rbxlx", ".lua", ".luau", ".txt")
        private val LUA_HINT = Regex("""\b(local|function|end|return|plugin|game|print|require)\b""")
        private val UNSAFE = Regex("[^A-Za-z0-9._ -]")
    }

    fun getDir(): File = dir.also { it.mkdirs() }

    fun list(): List<File> =
        getDir().listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    fun detect(bytes: ByteArray): String = detectKind(bytes).extension

    fun detectKind(input: ByteArray): PluginKind {
        val bytes = unwrap(input)

        if (startsWith(bytes, "<roblox!")) {
            return PluginKind.RBXM
        }

        val text = textPrefix(bytes)
        if (text.trimStart().startsWith("<roblox")) {
            return PluginKind.RBXMX
        }

        var controls = 0
        for (char in text) {
            if (char.code < 0x20 && char != '\n' && char != '\r' && char != '\t') {
                controls++
            }
        }
        if (controls < 4 && LUA_HINT.containsMatchIn(text)) {
            return PluginKind.LUA
        }

        throw PluginFormatException("Not a Roblox model or Lua plugin file")
    }

    /**
     * Installs bytes as `<sanitized base name>.<detected extension>` and enables it.
     * Base names are sanitized with `[^A-Za-z0-9._ -] -> _`, truncated to 60 chars,
     * and known extensions are stripped first.
     */
    fun install(bytes: ByteArray, fileName: String): File {
        val kind = detectKind(bytes)
        val base = baseName(fileName)
        val target = File(getDir(), "$base.${kind.extension}")
        target.writeBytes(bytes)
        setEnabled(target.name, true)
        log("Installed plugin ${target.name}")
        return target
    }

    fun remove(fileName: String) {
        File(getDir(), fileName).delete()
        prefs.remove(prefix(fileName))
    }

    fun isEnabled(fileName: String): Boolean = !prefs.getBoolean(prefix(fileName), false)

    fun setEnabled(fileName: String, enabled: Boolean) {
        prefs.putBoolean(prefix(fileName), !enabled)
    }

    private fun prefix(fileName: String): String = "off:$fileName"

    private fun baseName(fileName: String): String {
        var name = fileName.substringAfterLast('/').substringAfterLast('\\')
        val lower = name.lowercase()
        for (extension in KNOWN_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                name = name.dropLast(extension.length)
                break
            }
        }
        name = name.replace(UNSAFE, "_").trim().take(60)
        return if (name.isBlank()) "plugin" else name
    }

    private fun unwrap(bytes: ByteArray): ByteArray {
        if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            return try {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            } catch (_: IOException) {
                bytes
            }
        }
        return bytes
    }

    private fun textPrefix(bytes: ByteArray): String {
        val limit = minOf(bytes.size, 4096)
        return String(bytes, 0, limit, Charsets.ISO_8859_1)
    }

    private fun startsWith(bytes: ByteArray, prefix: String): Boolean {
        val expected = prefix.toByteArray(Charsets.ISO_8859_1)
        if (bytes.size < expected.size) return false
        for (index in expected.indices) {
            if (bytes[index] != expected[index]) return false
        }
        return true
    }
}
