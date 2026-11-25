package com.tetrazbloxy.mobilestudio.net

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/** Small synchronous HTTP helper. Call it from a worker thread or dispatcher. */
object Http {
    data class Response(
        val code: Int,
        val body: ByteArray,
        val error: String? = null
    ) {
        val ok: Boolean get() = error == null && code in 200..299

        fun text(): String = body.toString(Charsets.UTF_8)

        fun json(): org.json.JSONObject? = try {
            org.json.JSONObject(text())
        } catch (_: Throwable) {
            null
        }
    }

    fun request(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
        contentType: String? = null
    ): Response {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MobileStudio/2.5 (Android)")
                setRequestProperty("Accept-Encoding", "gzip")
                for ((key, value) in headers) {
                    setRequestProperty(key, value)
                }
                if (contentType != null) {
                    setRequestProperty("Content-Type", contentType)
                }
                if (body != null) {
                    doOutput = true
                    setFixedLengthStreamingMode(body.size)
                }
            }

            if (body != null) {
                connection.outputStream.use { it.write(body) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            var bytes = stream?.use { readAll(it) } ?: ByteArray(0)
            val encoding = connection.getHeaderField("Content-Encoding")
            if (encoding?.contains("gzip", ignoreCase = true) == true && bytes.size >= 2) {
                bytes = try {
                    GZIPInputStream(bytes.inputStream()).use { readAll(it) }
                } catch (_: Throwable) {
                    bytes
                }
            }
            Response(code, bytes)
        } catch (exception: Throwable) {
            Response(0, ByteArray(0), exception.message ?: exception.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    fun getJson(url: String, headers: Map<String, String> = emptyMap()): Response =
        request(url, headers = headers)

    private fun readAll(stream: java.io.InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
