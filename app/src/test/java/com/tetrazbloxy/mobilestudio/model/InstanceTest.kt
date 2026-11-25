package com.tetrazbloxy.mobilestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InstanceTest {
    @Test
    fun parentingMaintainsBothSides() {
        val model = Model("Model")
        val part = Part("Brick")
        model.addChild(part)

        assertSame(model, part.parent)
        assertEquals(1, model.children.size)
        assertEquals("Model.Brick", part.getFullName())

        part.parent = null
        assertEquals(0, model.children.size)
        assertNull(part.parent)
    }

    @Test
    fun descendantsAreDepthFirst() {
        val root = Model("Root")
        val part = Part("Part")
        val child = Instance("Attachment")
        root.addChild(part)
        part.addChild(child)

        assertEquals(listOf(part, child), root.getDescendants())
        assertTrue(child.isDescendantOf(root))
        assertFalse(root.isDescendantOf(child))
    }

    @Test
    fun recursiveLookupAndClassChecks() {
        val root = Model("Root")
        val part = Part("Part")
        root.addChild(part)

        assertSame(part, root.findFirstChild("Part"))
        assertSame(part, root.findFirstChild("Part", recursive = true))
        assertSame(part, root.findFirstChildWhichIsA("BasePart", recursive = true))
        assertTrue(part.isA("Part"))
        assertTrue(part.isA("BasePart"))
        assertTrue(part.isA("Instance"))
        assertFalse(part.isA("Script"))
    }

    @Test
    fun propertyBagAndSpecialProperties() {
        val part = Part("Part")
        assertEquals("Part", part.getProperty("Name"))
        assertEquals("Part", part.getProperty("ClassName"))

        part.setProperty("CustomValue", 42)
        assertEquals(42, part.getProperty("CustomValue"))
        assertTrue(part.hasProperty("CustomValue"))
        assertTrue(part.removeProperty("CustomValue"))
        assertFalse(part.hasProperty("CustomValue"))
        assertFalse(part.setProperty("ClassName", "NotAllowed"))
    }

    @Test
    fun changeListenerFiresOnMutations() {
        val part = Part("Part")
        val seen = mutableListOf<String>()
        val disconnect = part.onChanged { _, property -> seen.add(property) }

        part.name = "Renamed"
        part.name = "Renamed"
        part.setProperty("Anchored", true)
        disconnect()
        part.name = "AfterDisconnect"

        assertEquals(listOf("Name", "Anchored"), seen)
    }

    @Test
    fun cloneIsDeep() {
        val model = Model("Model")
        val part = Part("Brick")
        part.size = Vector3(4f, 1f, 2f)
        part.setProperty("Custom", "value")
        model.addChild(part)

        val copy = model.clone()
        assertEquals("Model", copy.name)
        assertEquals(1, copy.children.size)
        val copiedPart = copy.children.first() as BasePart
        assertEquals(Vector3(4f, 1f, 2f), copiedPart.size)
        assertEquals("value", copiedPart.getProperty("Custom"))
        assertFalse(part === copiedPart)
    }

    @Test
    fun dataModelServicesAreStable() {
        val dataModel = DataModel()
        val workspace = dataModel.getService("Workspace")
        assertSame(workspace, dataModel.getService("Workspace"))
        assertSame(workspace, dataModel.workspace)
        assertEquals("Workspace", workspace.className)
        assertTrue(ClassDb.isServiceName("Workspace"))
        assertTrue(ClassDb.isA("Part", "Instance"))
    }

    @Test
    fun partDataExtractsRenderFields() {
        val part = Part("Brick")
        part.size = Vector3(2f, 2f, 2f)
        part.color = Color3.fromRGB(10, 20, 30)
        part.material = Enums.Material.NEON
        part.shape = Enums.PartType.CYLINDER.value

        val data = PartData.from(part)
        assertEquals(Vector3(2f, 2f, 2f), data.size)
        assertEquals(Color3.fromRGB(10, 20, 30), data.color)
        assertEquals(288, data.material)
        assertEquals(2, data.shape)
        assertNotNull(PartData.INSTANCE)
    }
}
