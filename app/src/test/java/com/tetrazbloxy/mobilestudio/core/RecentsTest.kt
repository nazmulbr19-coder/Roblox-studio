package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsTest {
    private fun entry(
        name: String,
        placeId: Long = 0L,
        universeId: Long = 0L,
        uri: String = "",
        time: Long = 0L
    ) = RecentEntry(name, placeId, universeId, uri, time, 1L)

    @Test
    fun newestEntryIsFirst() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store) { 100L }
        recents.add(entry("A", placeId = 1L))
        recents.add(entry("B", placeId = 2L))

        assertEquals(listOf("B", "A"), recents.list().map { it.name })
    }

    @Test
    fun duplicatesAreReplacedAndMovedToFront() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store)
        recents.add(entry("A", placeId = 1L, time = 1L))
        recents.add(entry("B", placeId = 2L, time = 2L))
        recents.add(entry("A again", placeId = 1L, time = 3L))

        val list = recents.list()
        assertEquals(2, list.size)
        assertEquals("A again", list[0].name)
        assertEquals(3L, list[0].time)
    }

    @Test
    fun uriBasedEntriesDeduplicate() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store)
        val uri = "content://places/abc"
        recents.add(entry("Local", uri = uri, time = 1L))
        recents.add(entry("Local renamed", uri = uri, time = 2L))

        assertEquals(1, recents.list().size)
        assertEquals("Local renamed", recents.list().first().name)
    }

    @Test
    fun listIsCappedAtForty() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store)
        for (index in 1..45) {
            recents.add(entry("Place $index", placeId = index.toLong(), time = index.toLong()))
        }

        val list = recents.list()
        assertEquals(40, list.size)
        assertEquals(45L, list.first().placeId)
        assertEquals(6L, list.last().placeId)
    }

    @Test
    fun removeDeletesByKey() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store)
        recents.add(entry("A", placeId = 1L))
        recents.add(entry("B", placeId = 2L))
        recents.remove(entry("A", placeId = 1L))

        assertEquals(listOf("B"), recents.list().map { it.name })
    }

    @Test
    fun malformedJsonReturnsEmptyList() {
        val store = InMemoryKeyValueStore()
        store.putString(Recents.KEY, "[{broken")
        assertTrue(Recents(store).list().isEmpty())
    }

    @Test
    fun jsonSchemaUsesSpecFieldNames() {
        val store = InMemoryKeyValueStore()
        val recents = Recents(store)
        recents.add(RecentEntry("Place", 10L, 20L, "content://x", 30L, 40L))

        val raw = store.getString(Recents.KEY)!!.let { org.json.JSONArray(it).getJSONObject(0) }
        assertEquals("Place", raw.getString("name"))
        assertEquals(10L, raw.getLong("placeId"))
        assertEquals(20L, raw.getLong("universeId"))
        assertEquals("content://x", raw.getString("uri"))
        assertEquals(30L, raw.getLong("time"))
        assertEquals(40L, raw.getLong("userId"))
    }
}
