package com.tetrazbloxy.mobilestudio.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKeyValueStoreTest {
    @Test
    fun returnsDefaultsForMissingKeys() {
        val store = InMemoryKeyValueStore()
        assertNull(store.getString("missing"))
        assertEquals("fallback", store.getString("missing", "fallback"))
        assertEquals(true, store.getBoolean("missing", true))
        assertEquals(7, store.getInt("missing", 7))
        assertEquals(2.5f, store.getFloat("missing", 2.5f), 1e-6f)
        assertEquals(9L, store.getLong("missing", 9L))
        assertFalse(store.contains("missing"))
    }

    @Test
    fun storesAndReadsEveryType() {
        val store = InMemoryKeyValueStore()
        store.putString("s", "value")
        store.putBoolean("b", true)
        store.putInt("i", 42)
        store.putFloat("f", 1.5f)
        store.putLong("l", 9000000000L)

        assertEquals("value", store.getString("s"))
        assertEquals(true, store.getBoolean("b"))
        assertEquals(42, store.getInt("i"))
        assertEquals(1.5f, store.getFloat("f"), 1e-6f)
        assertEquals(9000000000L, store.getLong("l"))
        assertTrue(store.contains("s"))
    }

    @Test
    fun numericValuesCoerceAcrossTypes() {
        val store = InMemoryKeyValueStore()
        store.putLong("n", 12L)
        assertEquals(12, store.getInt("n"))
        assertEquals(12f, store.getFloat("n"), 1e-6f)

        store.putInt("m", 3)
        assertEquals(3L, store.getLong("m"))
    }

    @Test
    fun removeAndClear() {
        val store = InMemoryKeyValueStore()
        store.putString("a", "1")
        store.putString("b", "2")

        store.remove("a")
        assertFalse(store.contains("a"))
        assertTrue(store.contains("b"))

        store.clear()
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun allReturnsACopy() {
        val store = InMemoryKeyValueStore(mapOf("seed" to "value"))
        val snapshot = store.all()
        store.putString("later", "value")

        assertEquals("value", snapshot["seed"])
        assertFalse(snapshot.containsKey("later"))
        assertTrue(store.all().containsKey("later"))
    }
}
