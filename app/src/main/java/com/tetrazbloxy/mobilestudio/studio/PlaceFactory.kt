package com.tetrazbloxy.mobilestudio.studio

import com.tetrazbloxy.mobilestudio.model.CFrame
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.Enums
import com.tetrazbloxy.mobilestudio.model.Folder
import com.tetrazbloxy.mobilestudio.model.Humanoid
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.MeshPart
import com.tetrazbloxy.mobilestudio.model.Model
import com.tetrazbloxy.mobilestudio.model.Part
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.model.SpawnLocation
import com.tetrazbloxy.mobilestudio.model.Vector3
import kotlin.math.cos
import kotlin.math.sin

/** The three start-page templates (SPEC 5.1). */
enum class PlaceTemplate(val title: String, val description: String, val accent: Int) {
    BASEPLATE("Baseplate", "A wide, flat plate with a spawn point.", 0xFF5EEAD4.toInt()),
    CLASSIC("Classic", "Baseplate, spawn and a few sample parts.", 0xFFFBBF24.toInt()),
    EMPTY("Empty", "Services only. Build from nothing.", 0xFFA78BFA.toInt())
}

/** Creates new place documents and reusable sample content. */
object PlaceFactory {
    private val services = listOf(
        "Workspace", "Players", "Lighting", "ReplicatedFirst", "ReplicatedStorage",
        "ServerScriptService", "ServerStorage", "StarterGui", "StarterPack",
        "StarterPlayer", "Teams", "SoundService", "MaterialService", "TestService"
    )

    fun blank(): DataModel {
        val document = DataModel()
        for (service in services) {
            document.getService(service)
        }
        return document
    }

    fun newPlace(template: PlaceTemplate): DataModel {
        val document = blank()
        when (template) {
            PlaceTemplate.BASEPLATE -> addBaseplate(document)
            PlaceTemplate.CLASSIC -> {
                addBaseplate(document)
                addClassicSamples(document)
            }
            PlaceTemplate.EMPTY -> Unit
        }
        return document
    }

    private fun addBaseplate(document: DataModel) {
        val workspace = document.workspace
        val baseplate = Part("Baseplate")
        baseplate.size = Vector3(512f, 20f, 512f)
        baseplate.cframe = CFrame.new(0f, -10f, 0f)
        baseplate.color = Color3.fromRGB(163, 162, 165)
        baseplate.material = Enums.Material.BASEPLATE
        baseplate.anchored = true
        workspace.addChild(baseplate)

        val spawn = SpawnLocation("SpawnLocation")
        spawn.size = Vector3(12f, 1f, 12f)
        spawn.cframe = CFrame.new(0f, 0.5f, 0f)
        spawn.color = Color3.fromRGB(42, 163, 90)
        spawn.material = Enums.Material.NEON
        spawn.transparency = 0.25f
        spawn.anchored = true
        workspace.addChild(spawn)
    }

    private fun addClassicSamples(document: DataModel) {
        val workspace = document.workspace
        workspace.addChild(
            part("Red Block", Vector3(8f, 4f, 8f), Vector3(-18f, 2f, 6f), Color3.fromRGB(196, 40, 28))
        )
        workspace.addChild(
            part("Blue Sphere", Vector3(7f, 7f, 7f), Vector3(0f, 3.5f, 10f), Color3.fromRGB(13, 105, 172)).apply {
                shape = Enums.PartType.SPHERE.value
            }
        )
        workspace.addChild(
            part("Green Cylinder", Vector3(5f, 9f, 5f), Vector3(17f, 4.5f, 4f), Color3.fromRGB(75, 151, 75)).apply {
                shape = Enums.PartType.CYLINDER.value
            }
        )

        val script = Script("Hello")
        script.source = "print(\"Hello from Mobile Studio!\")\n"
        document.getService("ServerScriptService").addChild(script)
    }

    fun part(name: String, size: Vector3, position: Vector3, color: Color3): Part {
        val part = Part(name)
        part.size = size
        part.cframe = CFrame.new(position)
        part.color = color
        part.anchored = true
        return part
    }

    fun model(name: String, block: Model.() -> Unit = {}): Model {
        val model = Model(name)
        model.block()
        return model
    }

    fun folder(name: String): Folder = Folder(name)

    /** A small R15-style rig: HumanoidRootPart, torso, head, arms and legs. */
    fun createR15Rig(name: String = "R15"): Model {
        val model = Model(name)
        val root = Part("HumanoidRootPart")
        root.size = Vector3(2f, 2f, 1f)
        root.cframe = CFrame.new(0f, 3f, 0f)
        root.transparency = 1f
        root.anchored = true
        model.addChild(root)

        val humanoid = Humanoid()
        model.addChild(humanoid)

        fun limb(limbName: String, size: Vector3, offset: Vector3, color: Color3) {
            val part = Part(limbName)
            part.size = size
            part.cframe = CFrame.new(offset)
            part.color = color
            part.anchored = true
            model.addChild(part)
        }

        limb("Head", Vector3(2f, 1.6f, 1.6f), Vector3(0f, 5.8f, 0f), Color3.fromRGB(245, 205, 48))
        limb("UpperTorso", Vector3(2.4f, 2.2f, 1.2f), Vector3(0f, 4f, 0f), Color3.fromRGB(13, 105, 172))
        limb("LowerTorso", Vector3(2.2f, 1.6f, 1.1f), Vector3(0f, 2.5f, 0f), Color3.fromRGB(13, 105, 172))
        limb("LeftUpperArm", Vector3(1f, 1.8f, 1f), Vector3(-1.7f, 4f, 0f), Color3.fromRGB(245, 205, 48))
        limb("RightUpperArm", Vector3(1f, 1.8f, 1f), Vector3(1.7f, 4f, 0f), Color3.fromRGB(245, 205, 48))
        limb("LeftLowerArm", Vector3(0.9f, 1.7f, 0.9f), Vector3(-1.7f, 2.2f, 0f), Color3.fromRGB(245, 205, 48))
        limb("RightLowerArm", Vector3(0.9f, 1.7f, 0.9f), Vector3(1.7f, 2.2f, 0f), Color3.fromRGB(245, 205, 48))
        limb("LeftUpperLeg", Vector3(1.1f, 2f, 1.1f), Vector3(-0.65f, 1f, 0f), Color3.fromRGB(75, 151, 75))
        limb("RightUpperLeg", Vector3(1.1f, 2f, 1.1f), Vector3(0.65f, 1f, 0f), Color3.fromRGB(75, 151, 75))
        limb("LeftLowerLeg", Vector3(1f, 1.9f, 1f), Vector3(-0.65f, -0.9f, 0f), Color3.fromRGB(75, 151, 75))
        limb("RightLowerLeg", Vector3(1f, 1.9f, 1f), Vector3(0.65f, -0.9f, 0f), Color3.fromRGB(75, 151, 75))
        return model
    }

    fun circleOfParts(
        parent: Instance,
        count: Int,
        radius: Float,
        size: Vector3,
        y: Float = 2f
    ): List<Part> {
        val parts = mutableListOf<Part>()
        val safeCount = count.coerceIn(1, 64)
        for (index in 0 until safeCount) {
            val angle = (Math.PI * 2 * index / safeCount).toFloat()
            val part = Part("Part")
            part.size = size
            part.cframe = CFrame.new(cos(angle) * radius, y, sin(angle) * radius)
            part.color = Color3.fromRGB(
                (90 + index * 7) % 255,
                (120 + index * 11) % 255,
                (200 + index * 3) % 255
            )
            part.anchored = true
            parent.addChild(part)
            parts.add(part)
        }
        return parts
    }

    fun meshPart(name: String, meshId: String): MeshPart {
        val part = MeshPart(name)
        part.meshId = meshId
        return part
    }
}
