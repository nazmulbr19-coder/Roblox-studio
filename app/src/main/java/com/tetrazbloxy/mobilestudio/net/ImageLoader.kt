package com.tetrazbloxy.mobilestudio.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Roblox image loader with a memory LRU, a SHA-1 keyed PNG disk cache under
 * `cacheDir/img`, a 60 second failure window, and a strict Roblox-host check.
 */
class ImageLoader(context: Context) {
    companion object {
        const val FAIL_RETRY_MS = 60_000L
        const val MAX_BYTES = 8 * 1024 * 1024
        private val ROBLOX_HOSTS = listOf(
            "roblox.com", "rbxcdn.com", "robloxlabs.com", "rbx.com",
            "roblox.cn", "roblox.qq.com", "robloxdev.com"
        )
        private val ASSET_PATTERN = Regex(
            """(?:[?&]id=|/asset/|/library/|/catalog/|/store/asset/|/plugins/|/models/|rbxassetid://)(\d+)""",
            RegexOption.IGNORE_CASE
        )
    }

    private val appContext = context.applicationContext
    private val dir = File(appContext.cacheDir, "img")
    private val pool = Executors.newFixedThreadPool(3)
    private val memory = LruCache<String, Bitmap>(50_000)
    private val failed = ConcurrentHashMap<String, Long>()
    private val errors = ConcurrentHashMap<String, String>()
    @Volatile
    var epoch: Int = 0
        private set

    init {
        dir.mkdirs()
    }

    fun parseId(value: String?): Long? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.all { it.isDigit() }) return text.toLongOrNull()
        if (text.startsWith("rbxasset://", ignoreCase = true) &&
            !text.startsWith("rbxassetid://", ignoreCase = true)
        ) {
            return null
        }
        return ASSET_PATTERN.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }

    fun cacheKey(value: String?): String? {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        val id = parseId(text)
        if (id != null) return "asset:$id"
        if (text.startsWith("http://", true) || text.startsWith("https://", true)) {
            return "url:$text"
        }
        return null
    }

    fun cached(value: String?): Bitmap? {
        val key = cacheKey(value) ?: return null
        return memory.get(key)
    }

    /** Blocking load; call from IO. Returns null for non-Roblox or failed keys. */
    fun load(value: String?): Bitmap? {
        val key = cacheKey(value) ?: return null
        memory.get(key)?.let { return it }

        val lastFailure = failed[key]
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < FAIL_RETRY_MS) {
            return null
        }

        val file = diskFile(key)
        if (file.isFile && file.length() > 0) {
            decode(file.readBytes())?.let {
                memory.put(key, it)
                return it
            }
        }

        val bytes = try {
            fetch(key, value!!)
        } catch (exception: Throwable) {
            errors[key] = exception.message ?: exception.javaClass.simpleName
            failed[key] = System.currentTimeMillis()
            return null
        }
        if (bytes == null || bytes.isEmpty() || bytes.size > MAX_BYTES) {
            failed[key] = System.currentTimeMillis()
            return null
        }
        val bitmap = decode(bytes)
        if (bitmap == null) {
            failed[key] = System.currentTimeMillis()
            errors[key] = "not an image"
            return null
        }

        try {
            dir.mkdirs()
            val tmp = File(dir, "${sha1(key)}.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                file.writeBytes(bytes)
                tmp.delete()
            }
        } catch (_: Throwable) {
            // Cache writes are best effort.
        }

        failed.remove(key)
        errors.remove(key)
        memory.put(key, bitmap)
        epoch++
        return bitmap
    }

    fun request(value: String?, callback: (Bitmap?) -> Unit) {
        pool.execute {
            val bitmap = load(value)
            callback(bitmap)
        }
    }

    fun forget(value: String?) {
        val key = cacheKey(value) ?: return
        memory.remove(key)
        diskFile(key).delete()
        failed.remove(key)
        errors.remove(key)
    }

    fun lastError(value: String?): String? {
        val key = cacheKey(value) ?: return null
        return errors[key]
    }

    fun clear() {
        memory.evictAll()
        dir.listFiles()?.forEach { it.delete() }
        failed.clear()
        errors.clear()
    }

    private fun diskFile(key: String): File = File(dir, "${sha1(key)}.png")

    private fun fetch(key: String, original: String): ByteArray? {
        if (key.startsWith("asset:")) {
            val id = key.removePrefix("asset:")
            val response = Http.request("https://assetdelivery.roblox.com/v1/asset/?id=$id")
            return if (response.ok) response.body else null
        }
        val url = if (original.startsWith("http", true)) original else key.removePrefix("url:")
        if (!isRobloxHost(url)) {
            throw IllegalArgumentException("not a Roblox image")
        }
        val response = Http.request(url)
        return if (response.ok) response.body else null
    }

    fun isRobloxHost(url: String): Boolean {
        val host = try {
            URI(url).host?.lowercase() ?: return false
        } catch (_: Throwable) {
            return false
        }
        return ROBLOX_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private fun decode(bytes: ByteArray): Bitmap? = try {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (_: Throwable) {
        null
    }

    private fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
