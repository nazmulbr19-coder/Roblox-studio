package com.tetrazbloxy.mobilestudio.net

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Roblox account/session state backed by the `roblox_auth` preferences file
 * (SPEC 4.1). Cookies are stored without the `.ROBLOSECURITY=` prefix.
 */
class Accounts(private val store: KeyValueStore) {
    companion object {
        const val PREFS = "roblox_auth"
        const val COOKIE_HEADER = ".ROBLOSECURITY="
    }

    data class Account(
        val id: Long,
        val name: String,
        val displayName: String,
        val cookie: String
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("displayName", displayName)
            put("cookie", cookie)
        }

        companion object {
            fun fromJson(json: JSONObject): Account = Account(
                id = json.optLong("id", 0L),
                name = json.optString("name", ""),
                displayName = json.optString("displayName", ""),
                cookie = json.optString("cookie", "")
            )
        }
    }

    data class SignInResult(val success: Boolean, val message: String)

    var apiKey: String
        get() = store.getString("apiKey", "") ?: ""
        set(value) = store.putString("apiKey", value.trim())

    var cookie: String
        get() = sanitizeCookie(store.getString("cookie", "") ?: "")
        set(value) = store.putString("cookie", sanitizeCookie(value))

    var userId: Long
        get() = store.getLong("userId", 0L)
        set(value) = store.putLong("userId", value)

    var userName: String
        get() = store.getString("userName", "") ?: ""
        set(value) = store.putString("userName", value)

    var displayName: String
        get() = store.getString("displayName", "") ?: ""
        set(value) = store.putString("displayName", value)

    val signedIn: Boolean get() = cookie.isNotEmpty() || apiKey.isNotEmpty()

    val isCookieSession: Boolean get() = cookie.isNotEmpty()

    fun accounts(): List<Account> {
        val raw = store.getString("accounts", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<Account>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val account = Account.fromJson(item)
                if (account.name.isNotBlank() || account.id != 0L) {
                    result.add(account)
                }
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<Account>) {
        val array = JSONArray()
        for (account in accounts) array.put(account.toJson())
        store.putString("accounts", array.toString())
    }

    fun currentAccount(): Account? {
        if (!isCookieSession) return null
        return Account(userId, userName, displayName, cookie)
    }

    /**
     * Stores a cookie or Open Cloud API key. Cookie sessions are validated
     * against the Roblox authenticated-user endpoint.
     */
    suspend fun signIn(credential: String): SignInResult = withContext(Dispatchers.IO) {
        val raw = credential.trim()
        if (raw.isEmpty()) {
            return@withContext SignInResult(false, "Enter a .ROBLOSECURITY cookie or API key.")
        }

        if (!looksLikeCookie(raw)) {
            apiKey = raw
            if (userName.isBlank()) userName = "API Key"
            return@withContext SignInResult(
                true,
                "API key saved. Cookie sign-in unlocks cloud places and groups."
            )
        }

        val value = sanitizeCookie(raw)
        val response = Http.request(
            "https://users.roblox.com/v1/users/authenticated",
            headers = mapOf("Cookie" to COOKIE_HEADER + value)
        )
        if (!response.ok) {
            return@withContext SignInResult(
                false,
                "Roblox rejected this cookie (HTTP ${response.code})."
            )
        }
        val json = response.json()
            ?: return@withContext SignInResult(false, "Roblox returned an unreadable response.")

        val id = json.optLong("id", 0L)
        val name = json.optString("name", "")
        val display = json.optString("displayName", name)
        if (id == 0L || name.isBlank()) {
            return@withContext SignInResult(false, "Roblox did not return an account.")
        }

        cookie = value
        userId = id
        userName = name
        displayName = display
        addAccount(Account(id, name, display, value))
        SignInResult(true, "Signed in as @$name")
    }

    fun switchTo(account: Account): Boolean {
        if (account.cookie.isBlank()) return false
        cookie = account.cookie
        userId = account.id
        userName = account.name
        displayName = account.displayName
        return true
    }

    fun addAccount(account: Account) {
        val list = accounts().toMutableList()
        list.removeAll { it.id == account.id }
        list.add(0, account)
        saveAccounts(list)
    }

    fun removeAccount(id: Long) {
        saveAccounts(accounts().filterNot { it.id == id })
    }

    fun signOut() {
        store.remove("cookie")
        store.remove("userId")
        store.remove("userName")
        store.remove("displayName")
        apiKey = ""
    }

    fun clearAll() {
        store.clear()
    }

    private fun sanitizeCookie(value: String): String {
        var text = value.trim()
        if (text.startsWith(COOKIE_HEADER, ignoreCase = true)) {
            text = text.substring(COOKIE_HEADER.length)
        }
        return text.trim()
    }

    private fun looksLikeCookie(value: String): Boolean {
        val text = value.trim()
        return text.startsWith("_|") || text.startsWith(COOKIE_HEADER, ignoreCase = true) ||
            text.length > 80
    }
}
