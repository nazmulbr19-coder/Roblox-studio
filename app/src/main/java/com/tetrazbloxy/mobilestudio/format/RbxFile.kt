package com.tetrazbloxy.mobilestudio.format

import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.PropNames
import com.tetrazbloxy.mobilestudio.model.PropType
import com.tetrazbloxy.mobilestudio.model.RawPropValue
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

class RbxFormatException(message: String) : Exception(message)

enum class RbxFormat { BINARY, XML }

/**
 * Parsed document container. Body decoding of RBXL/RBXM is not part of this
 * first pass; [RbxFile.read] validates the container and records a warning.
 */
data class RbxDocument(
    val meta: MutableMap<String, String> = LinkedHashMap(),
    val roots: MutableList<Instance> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
    val format: RbxFormat? = null
)

/** RBXL/RBXM container detection, gzip unwrap, and material migration. */
object RbxFile {
    private const val BINARY_MAGIC = "<roblox!"

    /** Inflates a leading GZIP magic pair; otherwise returns the input unchanged. */
    fun unwrap(bytes: ByteArray): ByteArray {
        if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            return try {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            } catch (exception: IOException) {
                throw RbxFormatException("Corrupt gzip container: ${exception.message}")
            }
        }
        return bytes
    }

    /** Binary files must be longer than 14 bytes and start with `<roblox!`. */
    fun isBinary(bytes: ByteArray): Boolean {
        if (bytes.size <= 14) return false
        val magic = BINARY_MAGIC.toByteArray(Charsets.ISO_8859_1)
        if (bytes.size < magic.size) return false
        for (index in magic.indices) {
            if (bytes[index] != magic[index]) return false
        }
        return true
    }

    fun isXml(bytes: ByteArray): Boolean {
        if (isBinary(bytes)) return false
        val text = String(bytes, 0, minOf(bytes.size, 512), Charsets.ISO_8859_1)
        return text.trimStart().startsWith("<roblox")
    }

    fun read(bytes: ByteArray): RbxDocument {
        val data = unwrap(bytes)
        val format = when {
            isBinary(data) -> RbxFormat.BINARY
            isXml(data) -> RbxFormat.XML
            else -> throw RbxFormatException("Not a Roblox place or model file")
        }
        return RbxDocument(
            format = format,
            warnings = mutableListOf(
                "RBXL/RBXM body decoding is not implemented yet (container validated as $format)"
            )
        )
    }

    /** A document is a place when any root is a Workspace. */
    fun isPlace(document: RbxDocument): Boolean =
        document.roots.any { it.className == "Workspace" }

    /**
     * Migrates every legacy `Use2022Materials` boolean to the XML property
     * `Use2022MaterialsXml` (property type 2) and removes the old key.
     */
    fun normalize(document: RbxDocument) {
        val instances = mutableListOf<Instance>()
        for (root in document.roots) {
            instances.add(root)
            instances.addAll(root.getDescendants())
        }
        for (instance in instances) {
            if (!instance.hasProperty(PropNames.USE_2022_MATERIALS)) continue
            val value = instance.getProperty(PropNames.USE_2022_MATERIALS)
            instance.removeProperty(PropNames.USE_2022_MATERIALS)
            instance.setProperty(
                PropNames.USE_2022_MATERIALS_XML,
                RawPropValue(PropType.String, if (value == true) "true" else "false")
            )
        }
    }
}
