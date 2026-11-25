package com.tetrazbloxy.mobilestudio.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Roblox cloud client for the start page and account sheet.
 * Calls are synchronous HTTP on IO dispatchers and fail softly so the editor
 * stays usable offline.
 */
class RobloxCloud(private val accounts: Accounts) {
    data class CloudGame(
        val name: String,
        val placeId: Long,
        val universeId: Long,
        val creator: String = "",
        val playing: Int = 0,
        val visits: Long = 0,
        val thumbnailUrl: String? = null
    )

    data class GameGroup(
        val id: Long,
        val name: String,
        val role: String,
        val rank: Int,
        val games: List<CloudGame>
    )

    data class VersionEntry(val version: Int, val date: String, val placeId: Long = 0L)

    data class CloudResult<T>(val data: T? = null, val error: String? = null) {
        val ok: Boolean get() = data != null && error == null
    }

    private fun authHeaders(): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        if (accounts.cookie.isNotEmpty()) {
            headers["Cookie"] = Accounts.COOKIE_HEADER + accounts.cookie
        }
        if (accounts.apiKey.isNotEmpty()) {
            headers["x-api-key"] = accounts.apiKey
        }
        return headers
    }

    suspend fun myGames(limit: Int = 50): CloudResult<List<CloudGame>> =
        withContext(Dispatchers.IO) {
            if (!accounts.signedIn) {
                return@withContext CloudResult(error = "Sign in to see your games.")
            }
            val userId = accounts.userId
            if (userId == 0L) {
                return@withContext CloudResult(error = "This session has no user id.")
            }
            val url = "https://games.roblox.com/v2/users/$userId/games" +
                "?access=Own&limit=${limit.coerceIn(1, 50)}&sortOrder=Desc"
            val response = Http.request(url, headers = authHeaders())
            if (!response.ok) {
                return@withContext CloudResult(
                    error = "Could not load games (HTTP ${response.code})."
                )
            }
            parseGames(response.json())?.let { return@withContext CloudResult(it) }
            CloudResult(error = "Roblox returned an unreadable game list.")
        }

    suspend fun sharedGames(): CloudResult<List<CloudGame>> = withContext(Dispatchers.IO) {
        if (!accounts.signedIn) {
            return@withContext CloudResult(error = "Sign in to see shared games.")
        }
        val userId = accounts.userId
        if (userId == 0L) return@withContext CloudResult(error = "No user id in this session.")
        val url = "https://games.roblox.com/v2/users/$userId/games" +
            "?access=SharedWithMe&limit=50&sortOrder=Desc"
        val response = Http.request(url, headers = authHeaders())
        if (!response.ok) {
            return@withContext CloudResult(
                error = "Shared games are only available on the Roblox website."
            )
        }
        parseGames(response.json())?.let { return@withContext CloudResult(it) }
        CloudResult(error = "Roblox returned an unreadable game list.")
    }

    suspend fun groups(): CloudResult<List<GameGroup>> = withContext(Dispatchers.IO) {
        if (!accounts.signedIn || accounts.userId == 0L) {
            return@withContext CloudResult(error = "Sign in to see group games.")
        }
        val url = "https://groups.roblox.com/v1/users/${accounts.userId}/groups/roles"
        val response = Http.request(url, headers = authHeaders())
        if (!response.ok) {
            return@withContext CloudResult(error = "Could not load groups (HTTP ${response.code}).")
        }
        val root = response.json() ?: return@withContext CloudResult(error = "Unreadable group list.")
        val data = root.optJSONArray("data") ?: JSONArray()
        val groups = mutableListOf<GameGroup>()
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val group = item.optJSONObject("group") ?: continue
            val role = item.optJSONObject("role")
            val id = group.optLong("id", 0L)
            if (id == 0L) continue
            val games = groupGames(id).data ?: emptyList()
            groups.add(
                GameGroup(
                    id = id,
                    name = group.optString("name", "Group"),
                    role = role?.optString("name", "") ?: "",
                    rank = role?.optInt("rank", 0) ?: 0,
                    games = games
                )
            )
        }
        CloudResult(groups)
    }

    suspend fun groupGames(groupId: Long, limit: Int = 50): CloudResult<List<CloudGame>> =
        withContext(Dispatchers.IO) {
            val url = "https://games.roblox.com/v2/groups/$groupId/games" +
                "?access=Own&limit=${limit.coerceIn(1, 50)}&sortOrder=Desc"
            val response = Http.request(url, headers = authHeaders())
            if (!response.ok) {
                return@withContext CloudResult(
                    error = "Could not load group games (HTTP ${response.code})."
                )
            }
            parseGames(response.json())?.let { return@withContext CloudResult(it) }
            CloudResult(error = "Roblox returned an unreadable game list.")
        }

    suspend fun downloadPlace(placeId: Long): CloudResult<ByteArray> =
        withContext(Dispatchers.IO) {
            val url = "https://assetdelivery.roblox.com/v1/asset/?id=$placeId"
            val response = Http.request(url, headers = authHeaders())
            if (!response.ok || response.body.isEmpty()) {
                return@withContext CloudResult(
                    error = "Could not download place $placeId (HTTP ${response.code})."
                )
            }
            CloudResult(response.body)
        }

    suspend fun publishPlace(
        placeId: Long,
        universeId: Long,
        name: String,
        bytes: ByteArray
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        val url = "https://data.roblox.com/Data/Upload.ashx" +
            "?assetid=$placeId&type=Place&name=${encode(name)}"
        val response = Http.request(
            url,
            method = "POST",
            headers = authHeaders(),
            body = bytes,
            contentType = "application/octet-stream"
        )
        if (!response.ok) {
            return@withContext CloudResult(
                error = "Publish failed (HTTP ${response.code}). ${response.text().take(180)}"
            )
        }
        CloudResult(Unit)
    }

    suspend fun createPlace(name: String): CloudResult<CloudGame> = withContext(Dispatchers.IO) {
        val url = "https://data.roblox.com/Data/Upload.ashx?type=Place&name=${encode(name)}"
        val response = Http.request(
            url,
            method = "POST",
            headers = authHeaders(),
            body = ByteArray(0),
            contentType = "application/octet-stream"
        )
        if (!response.ok) {
            return@withContext CloudResult(error = "Create failed (HTTP ${response.code}).")
        }
        val json = response.json()
        val placeId = json?.optLong("assetId", 0L) ?: 0L
        val universeId = json?.optLong("universeId", 0L) ?: 0L
        if (placeId == 0L) {
            return@withContext CloudResult(
                error = "Roblox created the place but did not return an id."
            )
        }
        CloudResult(CloudGame(name, placeId, universeId))
    }

    suspend fun versionHistory(
        universeId: Long,
        placeId: Long
    ): CloudResult<List<VersionEntry>> = withContext(Dispatchers.IO) {
        val url = "https://develop.roblox.com/v1/universes/$universeId/places/$placeId/versions?limit=50"
        val response = Http.request(url, headers = authHeaders())
        if (!response.ok) {
            return@withContext CloudResult(error = "Version history is unavailable offline.")
        }
        val root = response.json() ?: return@withContext CloudResult(error = "Unreadable history.")
        val data = root.optJSONArray("data") ?: JSONArray()
        val versions = mutableListOf<VersionEntry>()
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            versions.add(
                VersionEntry(
                    version = item.optInt("versionNumber", index + 1),
                    date = item.optString("publishDate", ""),
                    placeId = placeId
                )
            )
        }
        CloudResult(versions)
    }

    fun thumbnailUrl(placeId: Long): String =
        "https://www.roblox.com/asset-thumbnail/image?assetId=$placeId&width=512&height=512&format=png"

    private fun parseGames(json: JSONObject?): List<CloudGame>? {
        if (json == null) return null
        val data = json.optJSONArray("data") ?: return null
        val games = mutableListOf<CloudGame>()
        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            val rootPlace = item.optJSONObject("rootPlace")
            val placeId = rootPlace?.optLong("id", 0L) ?: item.optLong("placeId", 0L)
            val universeId = item.optLong("id", 0L)
            val name = item.optString("name", "Untitled")
            games.add(
                CloudGame(
                    name = name,
                    placeId = placeId,
                    universeId = universeId,
                    creator = item.optJSONObject("creator")?.optString("name", "") ?: "",
                    playing = item.optInt("playing", 0),
                    visits = item.optLong("visits", 0L),
                    thumbnailUrl = if (placeId != 0L) thumbnailUrl(placeId) else null
                )
            )
        }
        return games
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
