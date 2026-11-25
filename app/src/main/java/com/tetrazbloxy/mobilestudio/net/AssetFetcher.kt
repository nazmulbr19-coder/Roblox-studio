package com.tetrazbloxy.mobilestudio.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.tetrazbloxy.mobilestudio.format.RbxFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Disk/bitmap cache for Roblox asset ids (SPEC 7.5).
 *
 * A fixed three-thread pool serves requests. Successful files are written to
 * `cacheDir/assets/<id>` through a `.tmp` file and renamed. Failures are
 * remembered for 90 seconds so the UI does not hammer a dead asset.
 */
class AssetFetcher(context: Context) {
    companion object {
        const val RETRY_MS = 90_000L
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
    }

    private val appContext = context.applicationContext
    private val dir = File(appContext.cacheDir, "assets")
    private val pool = Executors.newFixedThreadPool(3)
    private val pending = ConcurrentHashMap<Long, Boolean>()
    private val failed = ConcurrentHashMap<Long, Long>()
    private val errors = ConcurrentHashMap<Long, String>()

    private val bitmapCache: LruCache<Long, Bitmap> = object : LruCache<Long, Bitmap>(
        minOf(48 * 1024 * 1024, (Runtime.getRuntime().maxMemory() / 8).toInt())
    ) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    init {
        dir.mkdirs()
    }

    fun idOf(value: String?): Long? = AssetIds.parseAssetId(value)

    fun fileFor(id: Long): File = File(dir, id.toString())

    /** Returns cached bytes or null, scheduling a background download once. */
    fun request(id: Long): ByteArray? {
        val file = fileFor(id)
        if (file.isFile && file.length() > 0) {
            return try {
                file.readBytes()
            } catch (_: Throwable) {
                null
            }
        }
        val lastFailure = failed[id]
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < RETRY_MS) {
            return null
        }
        if (pending.putIfAbsent(id, true) != null) {
            return null
        }
        pool.execute {
            try {
                val bytes = download(id)
                if (bytes != null && bytes.isNotEmpty()) {
                    dir.mkdirs()
                    val tmp = File(dir, "$id.tmp")
                    tmp.writeBytes(bytes)
                    if (!tmp.renameTo(file)) {
                        file.writeBytes(bytes)
                        tmp.delete()
                    }
                    failed.remove(id)
                    errors.remove(id)
                } else {
                    failed[id] = System.currentTimeMillis()
                    errors[id] = "empty response"
                }
            } catch (exception: Throwable) {
                failed[id] = System.currentTimeMillis()
                errors[id] = exception.message ?: exception.javaClass.simpleName
            } finally {
                pending.remove(id)
            }
        }
        return null
    }

    /** Blocking variant used by explicit user actions such as plugin installs. */
    fun requestBlocking(id: Long): ByteArray? {
        val file = fileFor(id)
        if (file.isFile && file.length() > 0) {
            return try {
                file.readBytes()
            } catch (_: Throwable) {
                null
            }
        }
        return try {
            val bytes = download(id)
            if (bytes != null && bytes.isNotEmpty()) {
                dir.mkdirs()
                val tmp = File(dir, "$id.tmp")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(file)) {
                    file.writeBytes(bytes)
                    tmp.delete()
                }
                failed.remove(id)
                errors.remove(id)
                bytes
            } else {
                failed[id] = System.currentTimeMillis()
                null
            }
        } catch (exception: Throwable) {
            failed[id] = System.currentTimeMillis()
            errors[id] = exception.message ?: exception.javaClass.simpleName
            null
        }
    }

    fun isPending(id: Long): Boolean = pending.containsKey(id)

    fun lastError(id: Long): String? = errors[id]

    /** Decodes downloaded bytes as a bitmap, following a place's first image asset. */
    fun loadImage(id: Long, depth: Int = 3): Bitmap? {
        bitmapCache.get(id)?.let { return it }
        val bytes = requestBlocking(id) ?: return null
        if (bytes.size > MAX_IMAGE_BYTES) return null
        val bitmap = try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Throwable) {
            null
        }
        if (bitmap != null) {
            bitmapCache.put(id, bitmap)
            return bitmap
        }
        if (depth <= 1) return null
        return try {
            val document = RbxFile.read(bytes)
            val candidates = listOf(
                "ShirtTemplate", "PantsTemplate", "Graphic", "Texture",
                "TextureId", "Image", "ColorMap"
            )
            for (root in document.roots) {
                for (instance in listOf(root) + root.getDescendants()) {
                    for (key in candidates) {
                        val value = instance.getProperty(key)?.toString() ?: continue
                        val next = AssetIds.parseAssetId(value) ?: continue
                        if (next != id) return loadImage(next, depth - 1)
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    fun cachedBitmap(id: Long): Bitmap? = bitmapCache.get(id)

    private fun download(id: Long): ByteArray? {
        val response = Http.request(
            "https://assetdelivery.roblox.com/v1/asset/?id=$id",
            headers = mapOf("Accept" to "*/*")
        )
        if (!response.ok) {
            errors[id] = "HTTP ${response.code}"
            return null
        }
        return response.body
    }

    fun shutdown() {
        pool.shutdown()
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
