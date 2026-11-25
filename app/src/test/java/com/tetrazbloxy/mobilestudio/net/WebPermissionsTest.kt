package com.tetrazbloxy.mobilestudio.net

import com.tetrazbloxy.mobilestudio.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPermissionsTest {
    @Test
    fun robloxHostsAreAlwaysDeniedAndNeverPrompted() {
        val permissions = WebPermissions(InMemoryKeyValueStore())
        var prompts = 0
        permissions.asker = { _, _ -> prompts++ }

        assertTrue(permissions.isRobloxHost("roblox.com"))
        assertTrue(permissions.isRobloxHost("sub.roblox.com"))
        assertTrue(permissions.isRobloxHost("ROBLOX.COM."))
        assertTrue(permissions.isRobloxHost("roblox.qq.com"))
        assertFalse(permissions.isRobloxHost("notroblox.com"))
        assertFalse(permissions.isRobloxHost("example.com"))

        assertFalse(permissions.allow("roblox.com"))
        assertFalse(permissions.allow("game.rbxcdn.com"))
        assertEquals(0, prompts)
    }

    @Test
    fun storedDecisionIsReturnedWithoutPrompting() {
        val store = InMemoryKeyValueStore()
        store.putBoolean("example.com", true)
        val permissions = WebPermissions(store)
        var prompts = 0
        permissions.asker = { _, _ -> prompts++ }

        assertTrue(permissions.allow("Example.COM."))
        assertEquals(0, prompts)
        assertEquals(true, permissions.decision("example.com"))
    }

    @Test
    fun storedDenyIsHonoured() {
        val store = InMemoryKeyValueStore()
        store.putBoolean("example.com", false)
        val permissions = WebPermissions(store)
        var prompts = 0
        permissions.asker = { _, _ -> prompts++ }

        assertFalse(permissions.allow("example.com"))
        assertEquals(0, prompts)
    }

    @Test
    fun promptAllowPersistsDecision() {
        val store = InMemoryKeyValueStore()
        val permissions = WebPermissions(store)
        val prompted = mutableListOf<String>()
        permissions.asker = { host, _ ->
            prompted.add(host)
            permissions.answer(host, true)
        }

        assertTrue(permissions.allow("Example.COM."))
        assertEquals(listOf("example.com"), prompted)
        assertEquals(true, permissions.decision("example.com"))
        assertEquals(mapOf("example.com" to true), permissions.remembered())
    }

    @Test
    fun promptDenyPersistsDecision() {
        val permissions = WebPermissions(InMemoryKeyValueStore())
        permissions.asker = { host, _ -> permissions.answer(host, false) }

        assertFalse(permissions.allow("example.com"))
        assertEquals(false, permissions.decision("example.com"))
        assertEquals(false, permissions.remembered()["example.com"])
    }

    @Test
    fun dismissWithoutAnswerYieldsFalseAndStoresNothing() {
        val permissions = WebPermissions(InMemoryKeyValueStore())
        permissions.asker = { host, _ -> permissions.answer(host, null) }

        assertFalse(permissions.allow("example.com"))
        assertNull(permissions.decision("example.com"))
        assertTrue(permissions.remembered().isEmpty())
    }

    @Test
    fun noAskerYieldsFalse() {
        val permissions = WebPermissions(InMemoryKeyValueStore())
        assertFalse(permissions.allow("example.com"))
    }

    @Test
    fun normalizeHandlesSchemePortPathAndTrailingDot() {
        val permissions = WebPermissions(InMemoryKeyValueStore())
        assertEquals("example.com", permissions.normalize("HTTPS://Example.COM.:443/path?x=1"))
        assertEquals("example.com", permissions.normalize("example.com."))
        assertEquals("example.com", permissions.normalize("  Example.com  "))
    }

    @Test
    fun forgetAllClearsRememberedHosts() {
        val store = InMemoryKeyValueStore()
        store.putBoolean("a.com", true)
        store.putBoolean("b.com", false)
        val permissions = WebPermissions(store)

        assertEquals(2, permissions.remembered().size)
        permissions.forgetAll()
        assertTrue(permissions.remembered().isEmpty())
    }
}
