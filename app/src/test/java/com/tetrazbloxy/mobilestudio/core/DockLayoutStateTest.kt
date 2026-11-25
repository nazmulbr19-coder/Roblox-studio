package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.InMemoryKeyValueStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DockLayoutStateTest {
    @Test
    fun phoneDefaults() {
        val state = DockLayoutState(InMemoryKeyValueStore(), phone = true)
        assertEquals(220, state.leftW)
        assertEquals(250, state.rightW)
        assertEquals(140, state.bottomH)
        assertTrue(state.panel("explorer")!!.visible)
        assertTrue(state.panel("properties")!!.visible)
        assertFalse(state.panel("toolbox")!!.visible)
        assertFalse(state.panel("output")!!.visible)
    }

    @Test
    fun desktopDefaults() {
        val state = DockLayoutState(InMemoryKeyValueStore(), phone = false)
        assertEquals(270, state.leftW)
        assertEquals(320, state.rightW)
        assertEquals(190, state.bottomH)
        assertTrue(state.panels.all { it.visible })
    }

    @Test
    fun panelDefaultsMatchSpec() {
        val state = DockLayoutState(InMemoryKeyValueStore(), phone = false)
        assertEquals(Zone.RIGHT, state.panel("explorer")!!.zone)
        assertEquals(0, state.panel("explorer")!!.order)
        assertEquals(Zone.RIGHT, state.panel("properties")!!.zone)
        assertEquals(1, state.panel("properties")!!.order)
        assertEquals(Zone.LEFT, state.panel("toolbox")!!.zone)
        assertEquals(0, state.panel("toolbox")!!.order)
        assertEquals(Zone.BOTTOM, state.panel("output")!!.zone)
        assertEquals(0, state.panel("output")!!.order)
    }

    @Test
    fun saveWritesExactSchema() {
        val store = InMemoryKeyValueStore()
        val state = DockLayoutState(store, phone = false)
        state.leftW = 300
        state.save()

        val stored = store.getString(DockLayoutState.KEY)
        assertNotNull(stored)
        val root = JSONObject(stored!!)
        assertEquals(300, root.getInt("leftW"))
        assertEquals(320, root.getInt("rightW"))
        assertEquals(190, root.getInt("bottomH"))

        val panels = root.getJSONArray("panels")
        assertEquals(4, panels.length())
        val explorer = (0 until panels.length())
            .map { panels.getJSONObject(it) }
            .first { it.getString("id") == "explorer" }

        assertEquals("RIGHT", explorer.getString("zone"))
        assertEquals(0, explorer.getInt("order"))
        assertEquals(1.0, explorer.getDouble("weight"), 1e-6)
        assertTrue(explorer.getBoolean("visible"))
        assertEquals(-1, explorer.getInt("fx"))
        assertEquals(-1, explorer.getInt("fy"))
        assertEquals(0, explorer.getInt("fw"))
        assertEquals(0, explorer.getInt("fh"))
    }

    @Test
    fun loadClampsWeightsAndFallsBackForBadZones() {
        val store = InMemoryKeyValueStore()
        store.putString(
            DockLayoutState.KEY,
            """{"leftW":-5,"rightW":400,"bottomH":0,"panels":[
                {"id":"explorer","zone":"BOGUS","order":7,"weight":0.0001,"visible":false,
                 "fx":10,"fy":20,"fw":100,"fh":200},
                {"id":"unknownPanel","zone":"LEFT","order":0,"weight":1.0,"visible":true,
                 "fx":-1,"fy":-1,"fw":0,"fh":0}
            ]}"""
        )

        val state = DockLayoutState(store, phone = false)
        assertEquals(270, state.leftW)
        assertEquals(400, state.rightW)
        assertEquals(190, state.bottomH)

        val explorer = state.panel("explorer")!!
        assertEquals(Zone.RIGHT, explorer.zone)
        assertEquals(7, explorer.order)
        assertEquals(0.05f, explorer.weight, 1e-6f)
        assertFalse(explorer.visible)
        assertEquals(10, explorer.fx)
        assertEquals(20, explorer.fy)
        assertEquals(100, explorer.fw)
        assertEquals(200, explorer.fh)

        assertNull(state.panel("unknownPanel"))
        assertEquals(4, state.panels.size)
        assertEquals(1, state.panel("properties")!!.order)
    }

    @Test
    fun loadClampsHugeWeightToTwenty() {
        val store = InMemoryKeyValueStore()
        store.putString(
            DockLayoutState.KEY,
            """{"panels":[{"id":"output","zone":"FLOAT","weight":9999.0,"visible":true}]}"""
        )
        val state = DockLayoutState(store, phone = false)
        assertEquals(20f, state.panel("output")!!.weight, 1e-6f)
        assertEquals(Zone.FLOAT, state.panel("output")!!.zone)
    }

    @Test
    fun malformedJsonIsSwallowed() {
        val store = InMemoryKeyValueStore()
        store.putString(DockLayoutState.KEY, "{not json")
        val state = DockLayoutState(store, phone = false)
        assertEquals(270, state.leftW)
        assertEquals(4, state.panels.size)
    }

    @Test
    fun resetClearsStoredLayoutAndRestoresDefaults() {
        val store = InMemoryKeyValueStore()
        val state = DockLayoutState(store, phone = true)
        state.leftW = 999
        state.setAllHidden(true)
        state.save()
        assertTrue(store.contains(DockLayoutState.KEY))

        state.reset()
        assertFalse(store.contains(DockLayoutState.KEY))
        assertEquals(220, state.leftW)
        assertEquals(250, state.rightW)
        assertEquals(140, state.bottomH)
        assertTrue(state.panels.all { it.visible })
        assertTrue(state.panels.all { it.weight == 1f })
        assertFalse(state.getAllHidden())
    }

    @Test
    fun hideAndTogglePanels() {
        val state = DockLayoutState(InMemoryKeyValueStore(), phone = false)
        state.setAllHidden(true)
        assertTrue(state.getAllHidden())
        assertFalse(state.panel("explorer")!!.visible)

        state.setAllHidden(false)
        assertFalse(state.getAllHidden())

        state.toggle("properties")
        assertFalse(state.panel("properties")!!.visible)
        state.toggle("properties")
        assertTrue(state.panel("properties")!!.visible)
    }
}
