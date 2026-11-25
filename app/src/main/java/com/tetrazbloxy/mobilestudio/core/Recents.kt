package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

/** One entry of the recent-places list stored under `recents/list`. */
data class RecentEntry(
    val name: String,
    val placeId: Long,
    val universeId: Long,
    val uri: String,
    val time: Long,
    val userId: Long
) {
    /** De-duplication key: place id first, then persisted URI, then name. */
    fun key(): String = when {
        placeId != 0L -> "place:$placeId"
        uri.isNotEmpty() -> "uri:$uri"
        else -> "name:${name.lowercase()}"
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("placeId", placeId)
        put("universeId", universeId)
        put("uri", uri)
        put("time", time)
        put("userId", userId)
    }

    companion object {
        fun fromJson(json: JSONObject): RecentEntry = RecentEntry(
            name = json.optString("name", ""),
            placeId = json.optLong("placeId", 0L),
            universeId = json.optLong("universeId", 0L),
            uri = json.optString("uri", ""),
            time = json.optLong("time", 0L),
            userId = json.optLong("userId", 0L)
        )
    }
}

/**
 * Recent-places persistence in the `recents` SharedPreferences file.
 * De-duplicates by [RecentEntry.key], prepends newest, caps at 40.
 */
class Recents(
    private val store: KeyValueStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val PREFS = "recents"
        const val KEY = "list"
        const val MAX_ENTRIES = 40
    }

    fun list(): List<RecentEntry> {
        val raw = store.getString(KEY) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<RecentEntry>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                result.add(RecentEntry.fromJson(item))
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun add(entry: RecentEntry) {
        val key = entry.key()
        val entries = list()
            .filterNot { it.key() == key }
            .toMutableList()
        entries.add(0, entry)
        val capped = if (entries.size > MAX_ENTRIES) entries.subList(0, MAX_ENTRIES) else entries
        save(capped)
    }

    fun add(
        name: String,
        placeId: Long = 0L,
        universeId: Long = 0L,
        uri: String = "",
        userId: Long = 0L,
        time: Long = clock()
    ) {
        add(RecentEntry(name, placeId, universeId, uri, time, userId))
    }

    fun remove(entry: RecentEntry) {
        removeKey(entry.key())
    }

    fun removeKey(key: String) {
        save(list().filterNot { it.key() == key })
    }

    fun clear() {
        store.remove(KEY)
    }

    private fun save(entries: List<RecentEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            array.put(entry.toJson())
        }
        store.putString(KEY, array.toString())
    }
}
