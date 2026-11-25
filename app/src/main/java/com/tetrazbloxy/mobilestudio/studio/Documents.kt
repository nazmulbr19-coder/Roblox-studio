package com.tetrazbloxy.mobilestudio.studio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tetrazbloxy.mobilestudio.format.RbxFile
import com.tetrazbloxy.mobilestudio.format.RbxXmlCodec
import com.tetrazbloxy.mobilestudio.model.DataModel
import java.io.File

/** Encodes/decodes Mobile Studio documents (XML container, gzip aware). */
object DocumentCodec {
    fun encode(document: DataModel, name: String): String =
        RbxXmlCodec.encode(document, name)

    fun encodeSelection(instances: List<com.tetrazbloxy.mobilestudio.model.Instance>, name: String): String =
        RbxXmlCodec.encodeSelection(instances, name)

    fun decode(bytes: ByteArray): DataModel {
        if (bytes.isEmpty()) throw IllegalArgumentException("The file is empty.")
        val data = RbxFile.unwrap(bytes)
        if (!RbxFile.isXml(data)) {
            val kind = if (RbxFile.isBinary(data)) "binary RBXL/RBXM" else "unknown"
            throw IllegalArgumentException(
                "$kind files are validated but their bodies are not decoded yet. " +
                    "Open an XML (.rbxlx/.rbxmx) document for full editing."
            )
        }
        return RbxXmlCodec.decode(data.toString(Charsets.UTF_8))
    }
}

/** SAF helpers for reading, writing and naming documents. */
object DocumentFiles {
    fun read(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        return resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not open the selected file.")
    }

    fun write(context: Context, uri: Uri, bytes: ByteArray) {
        val resolver = context.contentResolver
        val stream = resolver.openOutputStream(uri, "wt")
            ?: throw IllegalArgumentException("Could not write to the selected file.")
        stream.use { it.write(bytes) }
    }

    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun takePermission(context: Context, uri: Uri): Boolean = try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        true
    } catch (_: Throwable) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun suggestedName(name: String, extension: String = "rbxl"): String {
        val cleaned = name.trim().ifBlank { "Untitled Place" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (cleaned.contains('.')) cleaned else "$cleaned.$extension"
    }

    fun sizeOf(context: Context, uri: Uri): Long? = try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) cursor.getLong(index) else null
            } else {
                null
            }
        }
    } catch (_: Throwable) {
        null
    }
}

/** A named group of parts/models saved from the toolbox. */
data class LibraryModel(val name: String, val file: File, val modified: Long)

/** Local model library under `filesDir/library` (RBXMX XML). */
class ModelLibrary(context: Context) {
    private val dir = File(context.filesDir, "library").apply { mkdirs() }

    fun list(): List<LibraryModel> =
        dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("rbxmx", true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { LibraryModel(it.nameWithoutExtension, it, it.lastModified()) }
            ?: emptyList()

    fun save(name: String, xml: String): LibraryModel? {
        val safe = name.trim().ifBlank { "Model" }
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .take(60)
        val file = File(dir, "$safe.rbxmx")
        return try {
            file.writeText(xml)
            LibraryModel(file.nameWithoutExtension, file, file.lastModified())
        } catch (_: Throwable) {
            null
        }
    }

    fun read(model: LibraryModel): String? = try {
        model.file.readText()
    } catch (_: Throwable) {
        null
    }

    fun delete(model: LibraryModel): Boolean = model.file.delete()
}
