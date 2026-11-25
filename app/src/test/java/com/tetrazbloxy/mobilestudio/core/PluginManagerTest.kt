package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class PluginManagerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun luaSource(): ByteArray =
        "local plugin = {}\nplugin.Name = \"Test\"\nreturn plugin\n".toByteArray()

    private fun binaryModel(): ByteArray {
        val bytes = ByteArray(20)
        "<roblox!".toByteArray(Charsets.ISO_8859_1).copyInto(bytes)
        return bytes
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private fun manager(store: InMemoryKeyValueStore = InMemoryKeyValueStore()): Pair<PluginManager, InMemoryKeyValueStore> =
        PluginManager(temp.newFolder(), store) to store

    @Test
    fun detectsBinaryXmlAndLua() {
        val (plugins, _) = manager()
        assertEquals("rbxm", plugins.detect(binaryModel()))
        assertEquals("rbxmx", plugins.detect("<roblox version=\"4\"></roblox>".toByteArray()))
        assertEquals("lua", plugins.detect(luaSource()))
    }

    @Test
    fun detectsGzippedLua() {
        val (plugins, _) = manager()
        assertEquals("lua", plugins.detect(gzip(luaSource())))
    }

    @Test
    fun rejectsGarbage() {
        val (plugins, _) = manager()
        assertThrows(PluginFormatException::class.java) {
            plugins.detect(ByteArray(8))
        }
    }

    @Test
    fun installSanitizesAndEnables() {
        val (plugins, store) = manager()
        val file = plugins.install(luaSource(), "C:\\Users\\me\\My Cool Plugin!.lua")

        assertEquals("My Cool Plugin_.lua", file.name)
        assertTrue(file.exists())
        assertEquals(1, plugins.list().size)
        assertTrue(plugins.isEnabled(file.name))
        assertFalse(store.getBoolean("off:${file.name}", true))
    }

    @Test
    fun installTruncatesLongNamesToSixtyChars() {
        val (plugins, _) = manager()
        val file = plugins.install(luaSource(), "x".repeat(80) + ".lua")
        assertEquals(60, file.nameWithoutExtension.length)
    }

    @Test
    fun installStripsBackslashesAndUsesDetectedExtension() {
        val (plugins, _) = manager()
        val file = plugins.install(binaryModel(), "folder/my-plugin.rbxl")
        assertEquals("my-plugin.rbxm", file.name)
    }

    @Test
    fun enabledStatePersistsInStore() {
        val store = InMemoryKeyValueStore()
        val (plugins, _) = manager(store)
        val file = plugins.install(luaSource(), "plugin.lua")

        plugins.setEnabled(file.name, false)
        assertFalse(plugins.isEnabled(file.name))
        assertTrue(store.getBoolean("off:${file.name}", false))

        val reloaded = PluginManager(plugins.getDir(), store)
        assertFalse(reloaded.isEnabled(file.name))

        reloaded.setEnabled(file.name, true)
        assertTrue(reloaded.isEnabled(file.name))
    }

    @Test
    fun removeDeletesFileAndPreference() {
        val (plugins, store) = manager()
        val file = plugins.install(luaSource(), "plugin.lua")
        plugins.setEnabled(file.name, false)

        plugins.remove(file.name)
        assertFalse(file.exists())
        assertTrue(plugins.list().isEmpty())
        assertFalse(store.contains("off:${file.name}"))
    }

    @Test
    fun listIsSortedCaseInsensitively() {
        val (plugins, _) = manager()
        plugins.install(luaSource(), "zebra.lua")
        plugins.install(luaSource(), "Apple.lua")
        plugins.install(luaSource(), "mango.lua")

        assertEquals(listOf("Apple.lua", "mango.lua", "zebra.lua"), plugins.list().map { it.name })
    }
}
