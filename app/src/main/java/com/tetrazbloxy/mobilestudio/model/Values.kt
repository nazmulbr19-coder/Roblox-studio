package com.tetrazbloxy.mobilestudio.model

/** A Roblox enum item: enum type name, item name, numeric value. */
data class EnumValue(val enumType: String, val name: String, val value: Int) {
    override fun toString(): String = "Enum.$enumType.$name"
}

/**
 * Enum items used by the data model and engine.
 * Values follow the SPEC tables where the SPEC pins them (Neon = 288,
 * part shapes 0..4, RunContext legacy = 0, Scriptable camera = 6).
 */
object Enums {
    object Material {
        val PLASTIC = EnumValue("Material", "Plastic", 256)
        val WOOD = EnumValue("Material", "Wood", 512)
        val WOOD_PLANKS = EnumValue("Material", "WoodPlanks", 528)
        val MARBLE = EnumValue("Material", "Marble", 784)
        val SLATE = EnumValue("Material", "Slate", 800)
        val CONCRETE = EnumValue("Material", "Concrete", 816)
        val GRANITE = EnumValue("Material", "Granite", 832)
        val BRICK = EnumValue("Material", "Brick", 848)
        val PEBBLE = EnumValue("Material", "Pebble", 864)
        val COBBLESTONE = EnumValue("Material", "Cobblestone", 880)
        val ROCK = EnumValue("Material", "Rock", 896)
        val SANDSTONE = EnumValue("Material", "Sandstone", 912)
        val BASEPLATE = EnumValue("Material", "Baseplate", 928)
        val CORRODED_METAL = EnumValue("Material", "CorrodedMetal", 1040)
        val DIAMOND_PLATE = EnumValue("Material", "DiamondPlate", 1056)
        val FOIL = EnumValue("Material", "Foil", 1072)
        val METAL = EnumValue("Material", "Metal", 1088)
        val GRASS = EnumValue("Material", "Grass", 1280)
        val SAND = EnumValue("Material", "Sand", 1296)
        val FABRIC = EnumValue("Material", "Fabric", 1312)
        val GLASS = EnumValue("Material", "Glass", 1568)
        val NEON = EnumValue("Material", "Neon", 288)
        val values = listOf(
            PLASTIC, WOOD, WOOD_PLANKS, MARBLE, SLATE, CONCRETE, GRANITE, BRICK,
            PEBBLE, COBBLESTONE, ROCK, SANDSTONE, BASEPLATE, CORRODED_METAL,
            DIAMOND_PLATE, FOIL, METAL, GRASS, SAND, FABRIC, GLASS, NEON
        )
    }

    object PartType {
        val SPHERE = EnumValue("PartType", "Ball", 0)
        val BLOCK = EnumValue("PartType", "Block", 1)
        val CYLINDER = EnumValue("PartType", "Cylinder", 2)
        val WEDGE = EnumValue("PartType", "Wedge", 3)
        val CORNER_WEDGE = EnumValue("PartType", "CornerWedge", 4)
        val values = listOf(SPHERE, BLOCK, CYLINDER, WEDGE, CORNER_WEDGE)
    }

    object RunContext {
        val LEGACY = EnumValue("RunContext", "Legacy", 0)
        val CLIENT = EnumValue("RunContext", "Client", 1)
        val PLUGIN = EnumValue("RunContext", "Plugin", 2)
        val SERVER = EnumValue("RunContext", "Server", 3)
        val values = listOf(LEGACY, CLIENT, PLUGIN, SERVER)
    }

    object CameraType {
        val FIXED = EnumValue("CameraType", "Fixed", 0)
        val ATTACH = EnumValue("CameraType", "Attach", 1)
        val WATCH = EnumValue("CameraType", "Watch", 2)
        val TRACK = EnumValue("CameraType", "Track", 3)
        val FOLLOW = EnumValue("CameraType", "Follow", 4)
        val CUSTOM = EnumValue("CameraType", "Custom", 5)
        val SCRIPTABLE = EnumValue("CameraType", "Scriptable", 6)
        val values = listOf(FIXED, ATTACH, WATCH, TRACK, FOLLOW, CUSTOM, SCRIPTABLE)
    }

    object EasingStyle {
        val LINEAR = EnumValue("EasingStyle", "Linear", 0)
        val SINE = EnumValue("EasingStyle", "Sine", 1)
        val BACK = EnumValue("EasingStyle", "Back", 2)
        val QUAD = EnumValue("EasingStyle", "Quad", 3)
        val QUART = EnumValue("EasingStyle", "Quart", 4)
        val QUINT = EnumValue("EasingStyle", "Quint", 5)
        val BOUNCE = EnumValue("EasingStyle", "Bounce", 6)
        val ELASTIC = EnumValue("EasingStyle", "Elastic", 7)
        val values = listOf(LINEAR, SINE, BACK, QUAD, QUART, QUINT, BOUNCE, ELASTIC)
    }

    val all: List<EnumValue>
        get() = Material.values + PartType.values + RunContext.values +
            CameraType.values + EasingStyle.values

    fun lookup(enumType: String, name: String): EnumValue? =
        all.firstOrNull { it.enumType == enumType && it.name.equals(name, ignoreCase = true) }

    fun valueOf(enumType: String, value: Int): EnumValue? =
        all.firstOrNull { it.enumType == enumType && it.value == value }
}

/** Text reference to an asset/content provider. */
data class ContentValue(val source: String = "") {
    companion object {
        val EMPTY = ContentValue("")
    }
}

/** Font family/weight/style descriptor. */
data class FontValue(val family: String = "", val weight: Int = 400, val style: Int = 0) {
    companion object {
        val DEFAULT = FontValue()
    }
}

/** Immutable byte payload used by BinaryString properties. */
class BinaryString(val bytes: ByteArray) {
    val size: Int get() = bytes.size

    override fun equals(other: Any?): Boolean =
        other is BinaryString && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "BinaryString(${bytes.size} bytes)"
}

/** Interned shared string value (Roblox SharedString). */
data class SharedStr(val value: String)

/** Unique identifier wrapper. */
data class UniqueId(val value: String) {
    companion object {
        fun random(): UniqueId = UniqueId(java.util.UUID.randomUUID().toString())
        fun new(): UniqueId = random()
    }
}

/** Physical surface/material properties used by the physics step. */
data class PhysicalProps(
    val density: Float = 0.7f,
    val friction: Float = 0.3f,
    val elasticity: Float = 0.5f,
    val frictionWeight: Float = 1f,
    val elasticityWeight: Float = 1f
)

/** Axial alignment flags. */
data class Axes(val x: Boolean, val y: Boolean, val z: Boolean) {
    companion object {
        val NONE = Axes(false, false, false)
        val ALL = Axes(true, true, true)
        fun new(x: Boolean = true, y: Boolean = true, z: Boolean = true) = Axes(x, y, z)
    }
}

/** Surface face flags. */
data class Faces(
    val top: Boolean,
    val bottom: Boolean,
    val left: Boolean,
    val right: Boolean,
    val front: Boolean,
    val back: Boolean
) {
    companion object {
        val NONE = Faces(false, false, false, false, false, false)
        val ALL = Faces(true, true, true, true, true, true)
        fun new(
            top: Boolean = true,
            bottom: Boolean = true,
            left: Boolean = true,
            right: Boolean = true,
            front: Boolean = true,
            back: Boolean = true
        ) = Faces(top, bottom, left, right, front, back)
    }
}

/** Ray with an origin and a direction. */
data class Ray(val origin: Vector3, val direction: Vector3) {
    val unit: Ray get() = Ray(origin, direction.unit)

    fun pointAt(distance: Float): Vector3 = origin + direction * distance

    companion object {
        fun new(origin: Vector3, direction: Vector3) = Ray(origin, direction)
    }
}

/** Security capability bitmask attached to plugin/sandbox instances. */
data class SecurityCaps(val value: Long) {
    fun has(capability: Long): Boolean = (value and capability) == capability

    companion object {
        const val NONE = 0L
        const val ALL = -1L
    }
}

/**
 * Untyped serialized property value.
 * PropType.String has code 2, matching the SPEC's Use2022MaterialsXml migration.
 */
data class RawPropValue(val type: PropType, val value: Any?)

enum class PropType(val code: Int) {
    Unknown(0),
    Bool(1),
    String(2),
    Int32(3),
    Float32(4),
    Float64(5),
    UDim(6),
    UDim2(7),
    Ray(8),
    Faces(9),
    Axes(10),
    BrickColor(11),
    Color3(12),
    Vector2(13),
    Vector3(14),
    CFrame(15),
    NumberSequence(16),
    ColorSequence(17),
    NumberRange(18),
    Content(19),
    Enum(20),
    BinaryString(21),
    SharedString(22),
    UniqueId(23),
    PhysicalProperties(24),
    Font(25),
    OptionalCFrame(26);

    companion object {
        fun fromCode(code: Int): PropType? = entries.firstOrNull { it.code == code }
    }
}

/** Canonical property names used throughout the model/storage layers. */
object PropNames {
    const val NAME = "Name"
    const val CLASS_NAME = "ClassName"
    const val PARENT = "Parent"
    const val SIZE = "Size"
    const val CFrame = "CFrame"
    const val POSITION = "Position"
    const val ROTATION = "Rotation"
    const val COLOR = "Color"
    const val MATERIAL = "Material"
    const val TRANSPARENCY = "Transparency"
    const val ANCHORED = "Anchored"
    const val SHAPE = "shape"
    const val SOURCE = "Source"
    const val DISABLED = "Disabled"
    const val ENABLED = "Enabled"
    const val RUN_CONTEXT = "RunContext"
    const val USE_2022_MATERIALS = "Use2022Materials"
    const val USE_2022_MATERIALS_XML = "Use2022MaterialsXml"
    const val CONTENT = "Content"
    const val MESH_CONTENT = "MeshContent"
    const val TEXTURE_CONTENT = "TextureContent"
}

/** Nullable CFrame wrapper used by optional serialized properties. */
data class OptionalCFrame(val cframe: CFrame?)
