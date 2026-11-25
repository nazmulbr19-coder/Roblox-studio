package com.tetrazbloxy.mobilestudio.studio

import com.tetrazbloxy.mobilestudio.model.CFrame
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.Enums
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.InstanceFactory
import com.tetrazbloxy.mobilestudio.model.Model
import com.tetrazbloxy.mobilestudio.model.Part
import com.tetrazbloxy.mobilestudio.model.Vector3

enum class ToolboxCategory(val title: String) {
    PARTS("Parts"),
    OBJECTS("Objects"),
    MODELS("Models"),
    AUDIO("Audio"),
    PLUGINS("Plugins"),
    LIBRARY("My Models"),
    INVENTORY("Inventory")
}

data class CatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val category: ToolboxCategory,
    val accent: Int,
    val builder: () -> Instance
)

data class AudioItem(
    val id: Long,
    val name: String,
    val creator: String,
    val genre: String,
    val seconds: Int
) {
    val url: String get() = "https://assetdelivery.roblox.com/v1/asset/?id=$id"
    val displayLength: String
        get() = "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Built-in toolbox content plus a small audio catalogue. */
object ToolboxCatalog {
    val items: List<CatalogItem> = listOf(
        CatalogItem(
            "part-block", "Block", "Standard 4×1×2 brick", ToolboxCategory.PARTS,
            0xFFF97316.toInt()
        ) {
            part("Part", Vector3(4f, 1f, 2f), Color3.fromRGB(163, 162, 165)).apply {
                material = Enums.Material.PLASTIC
            }
        },
        CatalogItem(
            "part-sphere", "Sphere", "Smooth ball part", ToolboxCategory.PARTS,
            0xFF38BDF8.toInt()
        ) {
            part("Sphere", Vector3(4f, 4f, 4f), Color3.fromRGB(80, 150, 220)).apply {
                shape = Enums.PartType.SPHERE.value
            }
        },
        CatalogItem(
            "part-cylinder", "Cylinder", "Rounded cylinder part", ToolboxCategory.PARTS,
            0xFF34D399.toInt()
        ) {
            part("Cylinder", Vector3(3f, 6f, 3f), Color3.fromRGB(90, 200, 150)).apply {
                shape = Enums.PartType.CYLINDER.value
            }
        },
        CatalogItem(
            "part-wedge", "Wedge", "Sloped wedge part", ToolboxCategory.PARTS,
            0xFFA78BFA.toInt()
        ) {
            val wedge = com.tetrazbloxy.mobilestudio.model.WedgePart("Wedge")
            wedge.size = Vector3(4f, 2f, 4f)
            wedge.cframe = CFrame.new(0f, 1f, 0f)
            wedge.color = Color3.fromRGB(150, 120, 220)
            wedge.anchored = true
            wedge
        },
        CatalogItem(
            "part-spawn", "Spawn", "Spawn location for playtests", ToolboxCategory.PARTS,
            0xFF4ADE80.toInt()
        ) {
            val spawn = com.tetrazbloxy.mobilestudio.model.SpawnLocation()
            spawn.size = Vector3(12f, 1f, 12f)
            spawn.cframe = CFrame.new(0f, 0.5f, 0f)
            spawn.color = Color3.fromRGB(42, 163, 90)
            spawn.material = Enums.Material.NEON
            spawn.transparency = 0.2f
            spawn.anchored = true
            spawn
        },
        CatalogItem(
            "part-neon", "Neon Block", "Glowing accent block", ToolboxCategory.PARTS,
            0xFFFBBF24.toInt()
        ) {
            part("Neon", Vector3(2f, 2f, 2f), Color3.fromRGB(255, 200, 40)).apply {
                material = Enums.Material.NEON
                transparency = 0.1f
            }
        },
        CatalogItem(
            "obj-script", "Script", "Server-side Lua script", ToolboxCategory.OBJECTS,
            0xFF60A5FA.toInt()
        ) {
            val script = com.tetrazbloxy.mobilestudio.model.Script("Script")
            script.source = "-- Write your code here\nprint(\"Hello, world!\")\n"
            script
        },
        CatalogItem(
            "obj-localscript", "LocalScript", "Client-side Lua script", ToolboxCategory.OBJECTS,
            0xFF818CF8.toInt()
        ) {
            val script = com.tetrazbloxy.mobilestudio.model.LocalScript("LocalScript")
            script.source = "-- Runs on the client\n"
            script
        },
        CatalogItem(
            "obj-folder", "Folder", "Organise your hierarchy", ToolboxCategory.OBJECTS,
            0xFF94A3B8.toInt()
        ) { InstanceFactory.create("Folder") },
        CatalogItem(
            "obj-model", "Model", "Group instances together", ToolboxCategory.OBJECTS,
            0xFF94A3B8.toInt()
        ) { InstanceFactory.create("Model") },
        CatalogItem(
            "obj-light", "Point Light", "A warm point light", ToolboxCategory.OBJECTS,
            0xFFFDE68A.toInt()
        ) {
            val light = InstanceFactory.create("PointLight")
            light.setProperty("Brightness", 2f)
            light.setProperty("Range", 24f)
            light.setProperty("Color", Color3.fromRGB(255, 220, 160))
            light
        },
        CatalogItem(
            "obj-weld", "Weld", "Weld constraint between parts", ToolboxCategory.OBJECTS,
            0xFFCBD5E1.toInt()
        ) { InstanceFactory.create("WeldConstraint") },
        CatalogItem(
            "model-house", "Cottage", "Walls, roof and a door", ToolboxCategory.MODELS,
            0xFFF87171.toInt()
        ) {
            model("Cottage") {
                addChild(part("Floor", Vector3(16f, 1f, 14f), Color3.fromRGB(120, 90, 70), Vector3(0f, 0.5f, 0f)))
                addChild(part("WallBack", Vector3(16f, 8f, 1f), Color3.fromRGB(230, 220, 200), Vector3(0f, 4f, -6.5f)))
                addChild(part("WallLeft", Vector3(1f, 8f, 14f), Color3.fromRGB(220, 210, 190), Vector3(-7.5f, 4f, 0f)))
                addChild(part("WallRight", Vector3(1f, 8f, 14f), Color3.fromRGB(220, 210, 190), Vector3(7.5f, 4f, 0f)))
                addChild(part("Door", Vector3(4f, 6f, 0.6f), Color3.fromRGB(120, 70, 45), Vector3(2f, 3f, 6.4f)))
                addChild(part("Roof", Vector3(18f, 1f, 16f), Color3.fromRGB(178, 60, 60), Vector3(0f, 8.5f, 0f)).apply {
                    shape = Enums.PartType.WEDGE.value
                })
            }
        },
        CatalogItem(
            "model-tower", "Tower", "Stacked accent blocks", ToolboxCategory.MODELS,
            0xFFA78BFA.toInt()
        ) {
            model("Tower") {
                for (level in 0 until 6) {
                    val size = Vector3(10f - level * 1.2f, 2f, 10f - level * 1.2f)
                    val block = part(
                        "Level${level + 1}",
                        size,
                        Color3.fromRGB(120 + level * 16, 100 + level * 10, 230 - level * 12),
                        Vector3(0f, level * 2.2f + 1f, 0f)
                    )
                    addChild(block)
                }
            }
        },
        CatalogItem(
            "model-obby", "Obby Start", "Three checkpoint platforms", ToolboxCategory.MODELS,
            0xFF34D399.toInt()
        ) {
            model("ObbyStart") {
                for (index in 0 until 4) {
                    addChild(
                        part(
                            "Platform${index + 1}",
                            Vector3(6f, 1f, 6f),
                            Color3.fromRGB(80 + index * 25, 200 - index * 18, 140 + index * 12),
                            Vector3(index * 10f, index * 2f + 1f, 0f)
                        )
                    )
                }
            }
        },
        CatalogItem(
            "model-tree", "Pine Tree", "Trunk and layered foliage", ToolboxCategory.MODELS,
            0xFF22C55E.toInt()
        ) {
            model("PineTree") {
                addChild(part("Trunk", Vector3(2f, 10f, 2f), Color3.fromRGB(110, 75, 50), Vector3(0f, 5f, 0f)))
                for (layer in 0 until 3) {
                    addChild(
                        part(
                            "Leaves${layer + 1}",
                            Vector3(9f - layer * 2f, 3f, 9f - layer * 2f),
                            Color3.fromRGB(40, 150, 80),
                            Vector3(0f, 9f + layer * 2.5f, 0f)
                        ).apply { shape = Enums.PartType.SPHERE.value }
                    )
                }
            }
        },
        CatalogItem(
            "model-r15", "R15 Rig", "A basic humanoid rig", ToolboxCategory.MODELS,
            0xFFFBBF24.toInt()
        ) { PlaceFactory.createR15Rig() }
    )

    val audio: List<AudioItem> = listOf(
        AudioItem(1837879082, "Soft Piano", "Roblox", "Ambient", 95),
        AudioItem(1837850070, "Night Ambience", "Roblox", "Ambient", 120),
        AudioItem(1841647335, "Upbeat Loop", "Roblox", "Electronic", 64),
        AudioItem(1839673167, "Retro Arcade", "Roblox", "Electronic", 48),
        AudioItem(1842760861, "Drum Loop", "Roblox", "Percussion", 32),
        AudioItem(1843449307, "Ocean Waves", "Roblox", "Nature", 76),
        AudioItem(1836317264, "Wind", "Roblox", "Nature", 88),
        AudioItem(1842045670, "Epic Brass", "Roblox", "Cinematic", 110),
        AudioItem(1843285031, "Mystery Pad", "Roblox", "Cinematic", 140),
        AudioItem(1840774500, "8-bit Victory", "Roblox", "Retro", 12)
    )

    fun byCategory(category: ToolboxCategory): List<CatalogItem> =
        items.filter { it.category == category }

    fun search(query: String, category: ToolboxCategory? = null): List<CatalogItem> {
        val text = query.trim()
        return items.filter { item ->
            (category == null || item.category == category) &&
                (text.isEmpty() || item.name.contains(text, true) ||
                    item.description.contains(text, true))
        }
    }

    fun part(name: String, size: Vector3, color: Color3, position: Vector3 = Vector3.ZERO): Part {
        val part = Part(name)
        part.size = size
        part.color = color
        part.cframe = CFrame.new(position)
        part.anchored = true
        return part
    }

    fun model(name: String, block: Model.() -> Unit): Model {
        val model = Model(name)
        model.block()
        return model
    }
}
