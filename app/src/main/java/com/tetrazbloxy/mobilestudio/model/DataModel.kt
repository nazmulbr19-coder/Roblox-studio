package com.tetrazbloxy.mobilestudio.model

/** Root document instance that owns services (Roblox DataModel). */
class DataModel : Instance("DataModel", "DataModel") {
    private val services = LinkedHashMap<String, Instance>()

    val workspace: Instance get() = getService("Workspace")

    fun getService(serviceName: String): Instance {
        services[serviceName]?.let { return it }
        val existing = findFirstChild(serviceName)
        if (existing != null) {
            services[serviceName] = existing
            return existing
        }
        val created = InstanceFactory.create(serviceName)
        created.name = serviceName
        created.parent = this
        services[serviceName] = created
        return created
    }

    fun findService(serviceName: String): Instance? =
        services[serviceName] ?: findFirstChild(serviceName)
}

/** Minimal class-inheritance registry used by [Instance.isA] and icon lookup. */
object ClassDb {
    private val parents: Map<String, String?> = mapOf(
        "Instance" to null,
        "DataModel" to "Instance",
        "Workspace" to "Instance",
        "Model" to "Instance",
        "Folder" to "Instance",
        "BasePart" to "Instance",
        "Part" to "BasePart",
        "MeshPart" to "BasePart",
        "WedgePart" to "BasePart",
        "TrussPart" to "BasePart",
        "SpawnLocation" to "Part",
        "Humanoid" to "Instance",
        "Script" to "Instance",
        "LocalScript" to "Script",
        "ModuleScript" to "Script",
        "Player" to "Instance",
        "Players" to "Instance",
        "Lighting" to "Instance",
        "ReplicatedFirst" to "Instance",
        "ReplicatedStorage" to "Instance",
        "ServerScriptService" to "Instance",
        "ServerStorage" to "Instance",
        "StarterGui" to "Instance",
        "StarterPack" to "Instance",
        "StarterPlayer" to "Instance",
        "StarterPlayerScripts" to "Instance",
        "StarterCharacterScripts" to "Instance",
        "Teams" to "Instance",
        "SoundService" to "Instance",
        "Chat" to "Instance",
        "TestService" to "Instance",
        "MaterialService" to "Instance",
        "HttpService" to "Instance",
        "RunService" to "Instance",
        "UserInputService" to "Instance",
        "TweenService" to "Instance",
        "CollectionService" to "Instance",
        "Selection" to "Instance",
        "Stats" to "Instance",
        "Camera" to "Instance",
        "Sound" to "Instance",
        "Decal" to "Instance",
        "Texture" to "Instance",
        "GuiObject" to "Instance",
        "Frame" to "GuiObject",
        "TextLabel" to "GuiObject",
        "TextButton" to "GuiObject",
        "TextBox" to "GuiObject",
        "ImageLabel" to "GuiObject",
        "ScreenGui" to "Instance",
        "BillboardGui" to "Instance",
        "SurfaceGui" to "Instance",
        "Constraint" to "Instance",
        "WeldConstraint" to "Constraint",
        "HingeConstraint" to "Constraint",
        "SpringConstraint" to "Constraint",
        "Motor6D" to "Instance"
    )

    private val serviceNames: Set<String> = setOf(
        "Workspace", "Players", "Lighting", "ReplicatedFirst", "ReplicatedStorage",
        "ServerScriptService", "ServerStorage", "StarterGui", "StarterPack",
        "StarterPlayer", "Teams", "SoundService", "Chat", "TestService",
        "MaterialService", "TextChatService", "HttpService", "RunService",
        "UserInputService", "TweenService", "CollectionService", "PathfindingService",
        "PhysicsService", "MarketplaceService", "TeleportService", "BadgeService",
        "DataStoreService", "MessagingService", "MemoryStoreService", "AssetService",
        "InsertService", "LocalizationService", "LogService", "NetworkClient",
        "NetworkServer", "PolicyService", "ProximityPromptService", "SocialService",
        "GuiService", "ContextActionService", "ContentProvider", "Selection", "Stats",
        "ScriptContext", "GeometryService", "AvatarCreationService", "StudioService",
        "PluginGuiService", "ChangeHistoryService", "DraggerService", "FileMeshService"
    )

    fun superclassOf(className: String): String? = parents[className]

    fun isA(className: String, targetClassName: String): Boolean {
        if (className == targetClassName) return true
        var current: String? = parents[className]
        var guard = 0
        while (current != null && guard++ < 64) {
            if (current == targetClassName) return true
            current = parents[current]
        }
        return false
    }

    fun isKnownClass(className: String): Boolean = parents.containsKey(className)

    /** All class names known to the local class database. */
    fun allClasses(): List<String> = parents.keys.sorted()

    fun isServiceName(name: String): Boolean = serviceNames.contains(name)
}

/** Creates the typed instance subclass for a class name. */
object InstanceFactory {
    fun create(className: String): Instance = when (className) {
        "DataModel" -> DataModel()
        "Workspace" -> Workspace()
        "Part" -> Part()
        "SpawnLocation" -> SpawnLocation()
        "WedgePart" -> WedgePart()
        "MeshPart" -> MeshPart()
        "Model" -> Model()
        "Folder" -> Folder()
        "Script" -> Script()
        "LocalScript" -> LocalScript()
        "ModuleScript" -> ModuleScript()
        "Humanoid" -> Humanoid()
        else -> Instance(className)
    }

    fun new(className: String): Instance = create(className)
}

open class BasePart(name: String = "Part", className: String = "Part") : Instance(className, name) {
    var size: Vector3 = Vector3.ONE
    var cframe: CFrame = CFrame.new()
    var color: Color3 = Color3.fromRGB(163, 162, 165)
    var material: EnumValue = Enums.Material.PLASTIC
    var transparency: Float = 0f
    var anchored: Boolean = false
    var shape: Int = Enums.PartType.BLOCK.value

    override fun copyStateTo(target: Instance) {
        if (target !is BasePart) return
        target.size = size
        target.cframe = cframe
        target.color = color
        target.material = material
        target.transparency = transparency
        target.anchored = anchored
        target.shape = shape
    }
}

class Part(name: String = "Part") : BasePart(name, "Part")

class SpawnLocation(name: String = "SpawnLocation") : BasePart(name, "SpawnLocation")

class WedgePart(name: String = "WedgePart") : BasePart(name, "WedgePart") {
    init {
        shape = Enums.PartType.WEDGE.value
    }
}

class MeshPart(name: String = "MeshPart") : BasePart(name, "MeshPart") {
    var meshId: String = ""
    var textureId: String = ""

    override fun copyStateTo(target: Instance) {
        super.copyStateTo(target)
        if (target !is MeshPart) return
        target.meshId = meshId
        target.textureId = textureId
    }
}

class Model(name: String = "Model") : Instance("Model", name)

class Folder(name: String = "Folder") : Instance("Folder", name)

class Workspace(name: String = "Workspace") : Instance("Workspace", name)

open class Script(
    name: String = "Script",
    className: String = "Script"
) : Instance(className, name) {
    var source: String = ""
    var disabled: Boolean = false
    var enabled: Boolean = true
    var runContext: EnumValue = Enums.RunContext.LEGACY

    override fun copyStateTo(target: Instance) {
        if (target !is Script) return
        target.source = source
        target.disabled = disabled
        target.enabled = enabled
        target.runContext = runContext
    }
}

class LocalScript(name: String = "LocalScript") : Script(name, "LocalScript")

class ModuleScript(name: String = "ModuleScript") : Script(name, "ModuleScript")

class Humanoid(name: String = "Humanoid") : Instance("Humanoid", name) {
    var health: Float = 100f
    var maxHealth: Float = 100f
    var walkSpeed: Float = 16f
    var jumpPower: Float = 50f
    var useJumpPower: Boolean = true
    var jumpHeight: Float = 7.2f
    var breakJointsOnDeath: Boolean = true

    override fun copyStateTo(target: Instance) {
        if (target !is Humanoid) return
        target.health = health
        target.maxHealth = maxHealth
        target.walkSpeed = walkSpeed
        target.jumpPower = jumpPower
        target.useJumpPower = useJumpPower
        target.jumpHeight = jumpHeight
        target.breakJointsOnDeath = breakJointsOnDeath
    }
}

/** Renderable geometry/material snapshot extracted from a part. */
data class PartData(
    val cframe: CFrame,
    val size: Vector3,
    val shape: Int,
    val color: Color3,
    val material: Int,
    val transparency: Float,
    val content: String,
    val meshContent: String,
    val textureContent: String
) {
    companion object {
        val INSTANCE = PartData(
            cframe = CFrame.new(),
            size = Vector3.ONE,
            shape = Enums.PartType.BLOCK.value,
            color = Color3.fromRGB(163, 162, 165),
            material = Enums.Material.PLASTIC.value,
            transparency = 0f,
            content = "",
            meshContent = "",
            textureContent = ""
        )

        fun from(part: BasePart): PartData = PartData(
            cframe = part.cframe,
            size = part.size,
            shape = part.shape,
            color = part.color,
            material = part.material.value,
            transparency = part.transparency,
            content = part.getProperty(PropNames.CONTENT)?.toString() ?: "",
            meshContent = part.getProperty(PropNames.MESH_CONTENT)?.toString() ?: "",
            textureContent = part.getProperty(PropNames.TEXTURE_CONTENT)?.toString() ?: ""
        )
    }
}
