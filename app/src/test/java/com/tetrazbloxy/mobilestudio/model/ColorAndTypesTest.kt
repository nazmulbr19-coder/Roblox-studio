package com.tetrazbloxy.mobilestudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ColorAndTypesTest {
    @Test
    fun color3ConvertsToAndFromArgb() {
        val color = Color3.fromRGB(255, 128, 0)
        assertEquals(0xFFFF8000.toInt(), color.toArgb())
        assertEquals(0xFFFF8000.toInt(), Color3.fromArgb(0xFFFF8000.toInt()).toArgb())
        assertEquals("#FF8000", color.toHex())
    }

    @Test
    fun color3HexParsing() {
        assertEquals(Color3.fromRGB(18, 52, 86), Color3.fromHex("#123456"))
        assertNull(Color3.fromHex("nope"))
        assertNull(Color3.fromHex("#FFF"))
    }

    @Test
    fun brickColorsLookupAndClosest() {
        assertNotNull(BrickColors.byName("White"))
        assertNotNull(BrickColors.byName("white"))
        assertEquals("White", BrickColors.closest(Color3.fromRGB(242, 243, 243)).name)
        assertEquals("Grey", BrickColors.name(194))
    }

    @Test
    fun numberFormatting() {
        assertEquals("1", DataTypes.fmtNum(1.0))
        assertEquals("1", DataTypes.fmtNum(1.0f))
        assertEquals("1.5", DataTypes.fmtNum(1.5))
        assertEquals("0.1", DataTypes.fmtNum(0.1f))
        assertEquals("0.123457", DataTypes.fmtNum(0.1234567))
        assertEquals("-2", DataTypes.fmtNum(-2.0))
        assertEquals("nan", DataTypes.fmtNum(Double.NaN))
    }

    @Test
    fun enumItemsAndPropTypes() {
        assertEquals(288, Enums.Material.NEON.value)
        assertEquals(Enums.Material.NEON, Enums.lookup("Material", "neon"))
        assertEquals(Enums.PartType.BLOCK.value, 1)
        assertEquals(Enums.RunContext.LEGACY.value, 0)
        assertEquals(Enums.CameraType.SCRIPTABLE.value, 6)
        assertEquals(PropType.String, PropType.fromCode(2))
        assertNull(PropType.fromCode(999))
    }

    @Test
    fun udimAndRectHelpers() {
        val size = UDim2.new(0.5f, 10f, 1f, -4f)
        assertEquals(0.5f, size.width, 1e-5f)
        assertEquals(1f, size.height, 1e-5f)
        assertEquals(UDim2.new(0f, 5f, 1f, 0f), UDim2.fromScale(0f, 1f) + UDim2.fromOffset(5f, 0f))

        val rect = Rect.new(0f, 0f, 10f, 4f)
        assertEquals(10f, rect.width, 1e-5f)
        assertEquals(4f, rect.height, 1e-5f)
        assertEquals(Vector2(5f, 2f), rect.center)
    }
}
