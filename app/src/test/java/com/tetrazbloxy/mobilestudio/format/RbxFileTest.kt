package com.tetrazbloxy.mobilestudio.format

import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.PropNames
import com.tetrazbloxy.mobilestudio.model.PropType
import com.tetrazbloxy.mobilestudio.model.RawPropValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class RbxFileTest {
    private fun binaryModel(): ByteArray {
        val bytes = ByteArray(20)
        "<roblox!".toByteArray(Charsets.ISO_8859_1).copyInto(bytes)
        return bytes
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    @Test
    fun binaryDetectionRequiresHeaderAndLength() {
        val fifteen = ByteArray(15)
        "<roblox!".toByteArray(Charsets.ISO_8859_1).copyInto(fifteen)
        assertTrue(RbxFile.isBinary(fifteen))

        val fourteen = ByteArray(14)
        "<roblox!".toByteArray(Charsets.ISO_8859_1).copyInto(fourteen)
        assertFalse(RbxFile.isBinary(fourteen))
        assertFalse(RbxFile.isBinary("hello".toByteArray()))
    }

    @Test
    fun xmlDetection() {
        assertTrue(RbxFile.isXml("<roblox version=\"4\"></roblox>".toByteArray()))
        assertTrue(RbxFile.isXml("  \n<roblox version=\"4\"></roblox>".toByteArray()))
        assertFalse(RbxFile.isXml(binaryModel()))
        assertFalse(RbxFile.isXml("hello".toByteArray()))
    }

    @Test
    fun unwrapInflatesGzip() {
        val payload = "hello rbxl".toByteArray()
        assertTrue(RbxFile.unwrap(gzip(payload)).contentEquals(payload))
    }

    @Test
    fun unwrapLeavesPlainBytesAlone() {
        val payload = "<roblox version=\"4\"></roblox>".toByteArray()
        assertTrue(RbxFile.unwrap(payload).contentEquals(payload))
    }

    @Test
    fun readValidatesContainerAndReportsFormat() {
        val xml = RbxFile.read("<roblox version=\"4\"></roblox>".toByteArray())
        assertEquals(RbxFormat.XML, xml.format)
        assertTrue(xml.warnings.isNotEmpty())

        val binary = RbxFile.read(binaryModel())
        assertEquals(RbxFormat.BINARY, binary.format)
    }

    @Test
    fun readRejectsUnknownContent() {
        assertThrows(RbxFormatException::class.java) {
            RbxFile.read("just some text".toByteArray())
        }
    }

    @Test
    fun isPlaceChecksForWorkspaceRoot() {
        val place = RbxDocument(roots = mutableListOf(Instance("Workspace")))
        assertTrue(RbxFile.isPlace(place))

        val model = RbxDocument(roots = mutableListOf(Instance("Model")))
        assertFalse(RbxFile.isPlace(model))
    }

    @Test
    fun normalizeMigratesMaterialProperty() {
        val materialService = Instance("MaterialService")
        materialService.setProperty(PropNames.USE_2022_MATERIALS, true)
        val document = RbxDocument(roots = mutableListOf(materialService))

        RbxFile.normalize(document)

        assertFalse(materialService.hasProperty(PropNames.USE_2022_MATERIALS))
        val migrated = materialService.getProperty(PropNames.USE_2022_MATERIALS_XML)
        assertTrue(migrated is RawPropValue)
        migrated as RawPropValue
        assertEquals(PropType.String, migrated.type)
        assertEquals(2, migrated.type.code)
        assertEquals("true", migrated.value)
    }
}
