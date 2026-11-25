package com.tetrazbloxy.mobilestudio.core

import com.tetrazbloxy.mobilestudio.storage.KeyValueStore
import org.json.JSONArray
import org.json.JSONObject

enum class Zone { LEFT, RIGHT, BOTTOM, FLOAT }

/** Persisted state of one docked/floating panel. */
data class DockPanel(
    val id: String,
    val zone: Zone,
    val order: Int,
    val weight: Float,
    val visible: Boolean,
    val fx: Int = -1,
    val fy: Int = -1,
    val fw: Int = 0,
    val fh: Int = 0
)

/**
 * Pure-state port of `ui.DockLayout` (SPEC 4.1).
 * Persists the exact `dock_v1` JSON schema, clamps weights to [0.05, 20],
 * uses phone defaults 220/250/140 (otherwise 270/320/190), and silently
 * ignores malformed JSON.
 */
class DockLayoutState(
    private val store: KeyValueStore,
    val phone: Boolean,
    private val onChanged: () -> Unit = {}
) {
    companion object {
        const val KEY = "dock_v1"
        const val MIN_WEIGHT = 0.05f
        const val MAX_WEIGHT = 20f

        val KNOWN_PANEL_IDS = listOf("explorer", "properties", "toolbox", "output")

        fun defaultZone(id: String): Zone = when (id) {
            "explorer", "properties" -> Zone.RIGHT
            "toolbox" -> Zone.LEFT
            "output" -> Zone.BOTTOM
            else -> Zone.LEFT
        }

        fun defaultOrder(id: String): Int = when (id) {
            "properties" -> 1
            else -> 0
        }

        fun clampWeight(value: Float): Float = value.coerceIn(MIN_WEIGHT, MAX_WEIGHT)
    }

    var leftW: Int = if (phone) 220 else 270
    var rightW: Int = if (phone) 250 else 320
    var bottomH: Int = if (phone) 140 else 190

    private val panelMap = LinkedHashMap<String, DockPanel>()

    init {
        for (id in KNOWN_PANEL_IDS) {
            panelMap[id] = defaultPanel(id)
        }
        load()
    }

    val panels: List<DockPanel> get() = panelMap.values.toList()

    fun panel(id: String): DockPanel? = panelMap[id]

    private fun hiddenByDefault(id: String): Boolean =
        phone && (id == "toolbox" || id == "output")

    private fun defaultPanel(id: String): DockPanel = DockPanel(
        id = id,
        zone = defaultZone(id),
        order = defaultOrder(id),
        weight = 1f,
        visible = !hiddenByDefault(id),
        fx = -1,
        fy = -1,
        fw = 0,
        fh = 0
    )

    fun addPanel(
        id: String,
        zone: Zone = defaultZone(id),
        order: Int = defaultOrder(id),
        weight: Float = 1f,
        visible: Boolean = !hiddenByDefault(id)
    ): DockPanel {
        panelMap[id]?.let { return it }
        val panel = DockPanel(id, zone, order, clampWeight(weight), visible)
        panelMap[id] = panel
        return panel
    }

    fun toggle(id: String) {
        val current = panelMap[id] ?: return
        panelMap[id] = current.copy(visible = !current.visible)
        onChanged()
    }

    fun setAllHidden(hidden: Boolean) {
        for (id in panelMap.keys.toList()) {
            val current = panelMap.getValue(id)
            panelMap[id] = current.copy(visible = !hidden)
        }
        onChanged()
    }

    fun getAllHidden(): Boolean = panelMap.values.all { !it.visible }

    /** Writes the exact `dock_v1` schema from SPEC 4.1. */
    fun save() {
        val root = JSONObject()
        root.put("leftW", leftW)
        root.put("rightW", rightW)
        root.put("bottomH", bottomH)

        val array = JSONArray()
        for (panel in panelMap.values) {
            val item = JSONObject()
            item.put("id", panel.id)
            item.put("zone", panel.zone.name)
            item.put("order", panel.order)
            item.put("weight", panel.weight.toDouble())
            item.put("visible", panel.visible)
            item.put("fx", panel.fx)
            item.put("fy", panel.fy)
            item.put("fw", panel.fw)
            item.put("fh", panel.fh)
            array.put(item)
        }
        root.put("panels", array)

        store.putString(KEY, root.toString())
        onChanged()
    }

    /** Loads `dock_v1`; unknown ids are ignored, absent ids keep defaults. */
    fun load() {
        val raw = store.getString(KEY) ?: return
        try {
            val root = JSONObject(raw)

            val loadedLeft = root.optInt("leftW", leftW)
            if (loadedLeft > 0) leftW = loadedLeft
            val loadedRight = root.optInt("rightW", rightW)
            if (loadedRight > 0) rightW = loadedRight
            val loadedBottom = root.optInt("bottomH", bottomH)
            if (loadedBottom > 0) bottomH = loadedBottom

            val array = root.optJSONArray("panels") ?: return
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id", "")
                val current = panelMap[id] ?: continue

                val loadedZone = item.optString("zone", "")
                val zone = Zone.entries.firstOrNull { it.name == loadedZone }
                    ?: defaultZone(id)

                val weight = item.optDouble("weight", current.weight.toDouble())
                    .toFloat()
                    .let { clampWeight(it) }

                panelMap[id] = current.copy(
                    zone = zone,
                    order = item.optInt("order", current.order),
                    weight = weight,
                    visible = item.optBoolean("visible", current.visible),
                    fx = item.optInt("fx", current.fx),
                    fy = item.optInt("fy", current.fy),
                    fw = item.optInt("fw", current.fw),
                    fh = item.optInt("fh", current.fh)
                )
            }
        } catch (_: Throwable) {
            // Malformed layout JSON is intentionally swallowed (SPEC 4.1).
        }
    }

    /** Removes `dock_v1` and restores default sizes/zones; all panels visible. */
    fun reset() {
        store.remove(KEY)
        leftW = if (phone) 220 else 270
        rightW = if (phone) 250 else 320
        bottomH = if (phone) 140 else 190

        for (id in KNOWN_PANEL_IDS) {
            panelMap[id] = defaultPanel(id).copy(visible = true, weight = 1f)
        }
        onChanged()
    }
}
