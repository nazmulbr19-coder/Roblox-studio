package com.tetrazbloxy.mobilestudio.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetIdsTest {
    @Test
    fun parsesPlainNumericId() {
        assertEquals(12345L, AssetIds.parseAssetId("12345"))
        assertEquals(12345L, AssetIds.parseAssetId("  12345  "))
    }

    @Test
    fun parsesUrlFormsFromSpec() {
        assertEquals(987654L, AssetIds.parseAssetId("https://www.roblox.com/library/987654/My-Place"))
        assertEquals(123L, AssetIds.parseAssetId("https://www.roblox.com/catalog/123/Item"))
        assertEquals(777L, AssetIds.parseAssetId("https://create.roblox.com/store/asset/777"))
        assertEquals(42L, AssetIds.parseAssetId("https://www.roblox.com/plugins/42/thing"))
        assertEquals(555L, AssetIds.parseAssetId("rbxassetid://555"))
        assertEquals(42L, AssetIds.parseAssetId("https://assetdelivery.roblox.com/v1/asset/?id=42"))
    }

    @Test
    fun rejectsNonAssetInput() {
        assertNull(AssetIds.parseAssetId(null))
        assertNull(AssetIds.parseAssetId(""))
        assertNull(AssetIds.parseAssetId("abc"))
        assertNull(AssetIds.parseAssetId("https://example.com/foo/123"))
        assertNull(AssetIds.parseAssetId("123abc"))
    }

    @Test
    fun convertsRobloxPriceCents() {
        assertEquals(100L, AssetIds.cents(1.0, 0))
        assertEquals(150L, AssetIds.cents(1.5, 0))
        assertEquals(3L, AssetIds.cents(2.5, -2))
        assertEquals(1L, AssetIds.cents(1.0, -2))
        assertEquals(0L, AssetIds.cents(0.0, 5))
    }

    @Test
    fun cloudPlaceCarriesIds() {
        val place = CloudPlace("Baseplate", 95206881L, 1234L)
        assertEquals("Baseplate", place.name)
        assertEquals(95206881L, place.placeId)
        assertEquals(1234L, place.universeId)
    }
}
