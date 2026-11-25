package com.tetrazbloxy.mobilestudio.net

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Web permission state machine (SPEC 7.4) backed by the `web_permissions` file.
 *
 * - Roblox-owned hosts are never prompted and always denied.
 * - A stored `true`/`false` decision is returned directly.
 * - Otherwise [asker] is invoked once per host and [allow] waits up to 2 minutes
 *   for [answer]; dismissing without an answer yields false.
 */
class WebPermissions(private val store: KeyValueStore) {
    companion object {
        const val PREFS = "web_permissions"
        const val WAIT_MINUTES = 2L

        private val ROBLOX_SUFFIXES = listOf(
            "roblox.com",
            "rbxcdn.com",
            "robloxlabs.com",
            "rbx.com",
            "roblox.cn",
            "roblox.qq.com",
            "robloxdev.com"
        )
    }

    /** Invoked with (host, who). It should show UI; the answer arrives via [answer]. */
    var asker: ((String, String?) -> Unit)? = null

    private val latches = ConcurrentHashMap<String, CountDownLatch>()
    private val asked = ConcurrentHashMap.newKeySet<String>()

    fun normalize(host: String): String {
        var value = host.trim().lowercase()
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.substringAfter('@')
        value = value.substringBefore('/')
        value = value.substringBefore('?')
        value = value.substringBefore('#')
        if (!value.startsWith("[")) {
            val colon = value.lastIndexOf(':')
            if (colon > 0 && value.indexOf(':') == colon) {
                value = value.substring(0, colon)
            }
        }
        while (value.endsWith('.')) {
            value = value.dropLast(1)
        }
        return value
    }

    fun isRobloxHost(host: String): Boolean {
        val value = normalize(host)
        if (value.isEmpty()) return false
        return ROBLOX_SUFFIXES.any { suffix ->
            value == suffix || value.endsWith(".$suffix")
        }
    }

    fun decision(host: String): Boolean? {
        val key = normalize(host)
        return if (store.contains(key)) store.getBoolean(key) else null
    }

    fun allow(host: String, who: String? = null): Boolean {
        val key = normalize(host)
        if (key.isEmpty()) return false
        if (isRobloxHost(key)) return false

        decision(key)?.let { return it }

        val latch = latches.computeIfAbsent(key) { CountDownLatch(1) }
        if (asked.add(key)) {
            val currentAsker = asker
            if (currentAsker == null) {
                answer(key, null)
            } else {
                try {
                    currentAsker(key, who)
                } catch (_: Throwable) {
                    answer(key, null)
                }
            }
        }

        try {
            latch.await(WAIT_MINUTES, TimeUnit.MINUTES)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        val result = decision(key)
        if (result == null) {
            latches.remove(key, latch)
            asked.remove(key)
        }
        return result == true
    }

    /** Records the user's answer (null = dismissed) and releases any waiter. */
    fun answer(host: String, allowed: Boolean?) {
        val key = normalize(host)
        if (allowed != null) {
            store.putBoolean(key, allowed)
        }
        asked.remove(key)
        latches.remove(key)?.countDown()
    }

    fun remembered(): Map<String, Boolean> {
        val result = LinkedHashMap<String, Boolean>()
        for ((key, value) in store.all()) {
            if (value is Boolean) {
                result[key] = value
            }
        }
        return result
    }

    fun forgetAll() {
        for (latch in latches.values) {
            latch.countDown()
        }
        latches.clear()
        asked.clear()
        store.clear()
    }
}
