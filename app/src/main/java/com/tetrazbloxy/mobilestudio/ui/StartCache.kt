package com.tetrazbloxy.mobilestudio.ui

import com.tetrazbloxy.mobilestudio.net.RobloxCloud
import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

/** Cached cloud lists for the start page (SPEC 4.1 `start_cache`). */
class StartCache(private val store: KeyValueStore) {
    companion object {
        const val PREFS = "start_cache"
    }

    enum class Kind(val key: String) {
        MINE("mine"),
        SHARED("shared")
    }

    fun load(kind: Kind, userId: Long): List<RobloxCloud.CloudGame> {
        val raw = store.getString("${kind.key}_$userId") ?: return emptyList()
        return try {
            gameList(JSONArray(raw))
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun save(kind: Kind, userId: Long, games: List<RobloxCloud.CloudGame>) {
        val array = JSONArray()
        for (game in games) array.put(gameJson(game))
        store.putString("${kind.key}_$userId", array.toString())
    }

    fun loadGroups(userId: Long): List<RobloxCloud.GameGroup> {
        val raw = store.getString("groups_$userId") ?: return emptyList()
        return try {
            val root = JSONObject(raw)
            val list = root.optJSONArray("list") ?: return emptyList()
            val result = mutableListOf<RobloxCloud.GameGroup>()
            for (index in 0 until list.length()) {
                val item = list.optJSONObject(index) ?: continue
                result.add(
                    RobloxCloud.GameGroup(
                        id = item.optLong("id", 0L),
                        name = item.optString("name", "Group"),
                        role = item.optString("role", ""),
                        rank = item.optInt("rank", 0),
                        games = gameList(item.optJSONArray("games") ?: JSONArray())
                    )
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveGroups(userId: Long, groups: List<RobloxCloud.GameGroup>) {
        val root = JSONObject().apply { put("in", groups.size) }
        val list = JSONArray()
        for (group in groups) {
            val item = JSONObject().apply {
                put("id", group.id)
                put("name", group.name)
                put("role", group.role)
                put("rank", group.rank)
                val games = JSONArray()
                for (game in group.games) games.put(gameJson(game))
                put("games", games)
            }
            list.put(item)
        }
        root.put("list", list)
        store.putString("groups_$userId", root.toString())
    }

    fun forget() {
        store.clear()
    }

    private fun gameList(array: JSONArray): List<RobloxCloud.CloudGame> {
        val games = mutableListOf<RobloxCloud.CloudGame>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            games.add(
                RobloxCloud.CloudGame(
                    name = item.optString("name", "Untitled"),
                    placeId = item.optLong("placeId", 0L),
                    universeId = item.optLong("universeId", 0L),
                    creator = item.optString("creator", ""),
                    playing = item.optInt("playing", 0),
                    visits = item.optLong("visits", 0L),
                    thumbnailUrl = item.optString("thumbnailUrl", "").takeIf { it.isNotBlank() }
                )
            )
        }
        return games
    }

    private fun gameJson(game: RobloxCloud.CloudGame): JSONObject = JSONObject().apply {
        put("name", game.name)
        put("placeId", game.placeId)
        put("universeId", game.universeId)
        put("creator", game.creator)
        put("playing", game.playing)
        put("visits", game.visits)
        put("thumbnailUrl", game.thumbnailUrl ?: "")
    }
}
