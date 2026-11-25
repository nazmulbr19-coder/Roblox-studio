package com.tetrazbloxy.mobilestudio.format

import android.util.Xml
import com.tetrazbloxy.mobilestudio.model.BasePart
import com.tetrazbloxy.mobilestudio.model.CFrame
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.EnumValue
import com.tetrazbloxy.mobilestudio.model.Enums
import com.tetrazbloxy.mobilestudio.model.Humanoid
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.InstanceFactory
import com.tetrazbloxy.mobilestudio.model.MeshPart
import com.tetrazbloxy.mobilestudio.model.RawPropValue
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.model.UDim2
import com.tetrazbloxy.mobilestudio.model.Vector3
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Small, human-readable RBXLX/RBXM-XML codec for the editor's own documents.
 *
 * The binary Roblox container reader is still detection-only in this project,
 * so this codec exists to make new places, saves, autosaves and model export
 * fully round-trip inside Mobile Studio. It understands the typed fields used
 * by the editor's model classes plus primitive property-bag values.
 */
object RbxXmlCodec {
    fun encode(document: DataModel, name: String = document.name): String {
        val out = StringBuilder()
        out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        out.append("<roblox version=\"4\">\n")
        writeInstance(out, document, name, 1)
        out.append("</roblox>\n")
        return out.toString()
    }

    fun encodeSelection(instances: List<Instance>, name: String = "Selection"): String {
        val out = StringBuilder()
        out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        out.append("<roblox version=\"4\">\n")
        if (instances.isEmpty()) {
            out.append("  <!-- empty selection -->\n")
        } else {
            for (instance in instances) {
                writeInstance(out, instance, instance.name, 1)
            }
        }
        out.append("</roblox>\n")
        return out.toString()
    }

    fun decode(xml: String): DataModel {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val stack = ArrayDeque<Instance>()
        var document: DataModel? = null
        var inProperties = false
        var propertyTag: String? = null
        var propertyName: String? = null
        var propertyEnum: String? = null
        var propertyText = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    when {
                        tag == "Item" -> {
                            val className = parser.getAttributeValue(null, "class") ?: "Instance"
                            val itemName = parser.getAttributeValue(null, "name") ?: className
                            val instance: Instance = if (stack.isEmpty()) {
                                if (className == "DataModel") {
                                    val model = DataModel()
                                    document = model
                                    model
                                } else {
                                    val model = document ?: DataModel().also { document = it }
                                    val child = InstanceFactory.create(className)
                                    model.workspace.addChild(child)
                                    child
                                }
                            } else {
                                InstanceFactory.create(className)
                            }
                            instance.name = itemName
                            if (stack.isNotEmpty()) {
                                stack.last().addChild(instance)
                            }
                            stack.addLast(instance)
                        }

                        tag == "Properties" -> inProperties = true

                        inProperties -> {
                            propertyTag = tag
                            propertyName = parser.getAttributeValue(null, "name")
                            propertyEnum = parser.getAttributeValue(null, "enum")
                            propertyText = StringBuilder()
                        }
                    }
                }

                XmlPullParser.TEXT, XmlPullParser.CDATA -> {
                    if (propertyTag != null) propertyText.append(parser.text)
                }

                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    when {
                        tag == "Item" -> if (stack.isNotEmpty()) stack.removeLast()
                        tag == "Properties" -> inProperties = false
                        inProperties && tag == propertyTag -> {
                            val target = stack.lastOrNull()
                            val key = propertyName
                            if (target != null && key != null) {
                                applyProperty(
                                    target,
                                    key,
                                    tag,
                                    propertyEnum,
                                    propertyText.toString()
                                )
                            }
                            propertyTag = null
                            propertyName = null
                            propertyEnum = null
                            propertyText = StringBuilder()
                        }
                    }
                }
            }
            event = parser.next()
        }

        return document ?: error("RBXLX document did not contain a DataModel")
    }

    private fun writeInstance(
        out: StringBuilder,
        instance: Instance,
        overrideName: String?,
        depth: Int
    ) {
        val pad = "  ".repeat(depth)
        out.append(pad)
            .append("<Item class=\"")
            .append(esc(instance.className))
            .append("\" name=\"")
            .append(esc(overrideName ?: instance.name))
            .append("\">\n")
        writeProperties(out, instance, depth + 1)
        for (child in instance.children.toList()) {
            writeInstance(out, child, child.name, depth + 1)
        }
        out.append(pad).append("</Item>\n")
    }

    private fun writeProperties(out: StringBuilder, instance: Instance, depth: Int) {
        val pad = "  ".repeat(depth)
        out.append(pad).append("<Properties>\n")
        val written = mutableSetOf<String>()

        fun prop(tag: String, name: String, value: String, enumType: String? = null) {
            written.add(name)
            out.append(pad).append("  <").append(tag)
            out.append(" name=\"").append(esc(name)).append('"')
            if (enumType != null) out.append(" enum=\"").append(esc(enumType)).append('"')
            out.append('>').append(esc(value)).append("</").append(tag).append(">\n")
        }

        if (instance is BasePart) {
            prop("Vector3", "Size", vec(instance.size))
            prop("CFrame", "CFrame", frame(instance.cframe))
            prop("Color3", "Color", color(instance.color))
            prop("int", "Material", instance.material.value.toString())
            prop("float", "Transparency", instance.transparency.toString())
            prop("bool", "Anchored", instance.anchored.toString())
            prop("int", "shape", instance.shape.toString())
        }
        if (instance is MeshPart) {
            prop("string", "MeshId", instance.meshId)
            prop("string", "TextureId", instance.textureId)
        }
        if (instance is Script) {
            prop("string", "Source", instance.source)
            prop("bool", "Disabled", instance.disabled.toString())
            prop("bool", "Enabled", instance.enabled.toString())
            prop("int", "RunContext", instance.runContext.value.toString())
        }
        if (instance is Humanoid) {
            prop("float", "Health", instance.health.toString())
            prop("float", "MaxHealth", instance.maxHealth.toString())
            prop("float", "WalkSpeed", instance.walkSpeed.toString())
            prop("float", "JumpPower", instance.jumpPower.toString())
            prop("bool", "UseJumpPower", instance.useJumpPower.toString())
            prop("float", "JumpHeight", instance.jumpHeight.toString())
            prop("bool", "BreakJointsOnDeath", instance.breakJointsOnDeath.toString())
        }

        for ((key, value) in instance.getProperties()) {
            if (key in written || value == null) continue
            writeBag(out, pad + "  ", key, value)
        }
        out.append(pad).append("</Properties>\n")
    }

    private fun writeBag(out: StringBuilder, pad: String, key: String, value: Any) {
        fun put(tag: String, text: String, enumType: String? = null) {
            out.append(pad).append('<').append(tag)
                .append(" name=\"").append(esc(key)).append('"')
            if (enumType != null) out.append(" enum=\"").append(esc(enumType)).append('"')
            out.append('>').append(esc(text)).append("</").append(tag).append(">\n")
        }
        when (value) {
            is String -> put("string", value)
            is Boolean -> put("bool", value.toString())
            is Int -> put("int", value.toString())
            is Long -> put("double", value.toString())
            is Float -> put("float", value.toString())
            is Double -> put("double", value.toString())
            is Vector3 -> put("Vector3", vec(value))
            is Color3 -> put("Color3", color(value))
            is UDim2 -> put(
                "UDim2",
                listOf(value.x.scale, value.x.offset, value.y.scale, value.y.offset)
                    .joinToString(",")
            )
            is EnumValue -> put("enum", value.name, value.enumType)
            is RawPropValue -> put(
                if (value.type.code == 1) "bool" else "string",
                value.value?.toString() ?: ""
            )
            else -> put("string", value.toString())
        }
    }

    private fun applyProperty(
        instance: Instance,
        key: String,
        tag: String,
        enumType: String?,
        raw: String
    ) {
        val text = raw.trim()
        when (key) {
            "Name" -> instance.name = raw
            "Size" -> (instance as? BasePart)?.size = parseVector3(text)
            "CFrame" -> (instance as? BasePart)?.cframe = parseCFrame(text)
            "Color" -> (instance as? BasePart)?.color = parseColor3(text)
            "Material" -> {
                val value = text.toIntOrNull() ?: Enums.Material.PLASTIC.value
                (instance as? BasePart)?.material =
                    Enums.valueOf("Material", value) ?: Enums.Material.PLASTIC
            }

            "Transparency" -> (instance as? BasePart)?.transparency =
                text.toFloatOrNull() ?: 0f

            "Anchored" -> (instance as? BasePart)?.anchored = text.toBoolean()
            "shape" -> (instance as? BasePart)?.shape =
                text.toIntOrNull() ?: Enums.PartType.BLOCK.value

            "MeshId" -> (instance as? MeshPart)?.meshId = raw
            "TextureId" -> (instance as? MeshPart)?.textureId = raw
            "Source" -> (instance as? Script)?.source = raw
            "Disabled" -> (instance as? Script)?.disabled = text.toBoolean()
            "Enabled" -> (instance as? Script)?.enabled = text.toBoolean()
            "RunContext" -> {
                val value = text.toIntOrNull() ?: 0
                (instance as? Script)?.runContext =
                    Enums.valueOf("RunContext", value) ?: Enums.RunContext.LEGACY
            }

            "Health" -> (instance as? Humanoid)?.health = text.toFloatOrNull() ?: 100f
            "MaxHealth" -> (instance as? Humanoid)?.maxHealth = text.toFloatOrNull() ?: 100f
            "WalkSpeed" -> (instance as? Humanoid)?.walkSpeed = text.toFloatOrNull() ?: 16f
            "JumpPower" -> (instance as? Humanoid)?.jumpPower = text.toFloatOrNull() ?: 50f
            "UseJumpPower" -> (instance as? Humanoid)?.useJumpPower = text.toBoolean()
            "JumpHeight" -> (instance as? Humanoid)?.jumpHeight = text.toFloatOrNull() ?: 7.2f
            "BreakJointsOnDeath" -> (instance as? Humanoid)?.breakJointsOnDeath =
                text.toBoolean()

            else -> instance.setProperty(key, decodeValue(tag, enumType, text))
        }
    }

    private fun decodeValue(tag: String, enumType: String?, text: String): Any? = when (tag) {
        "string" -> text
        "bool" -> text.toBoolean()
        "int" -> text.toIntOrNull() ?: 0
        "float" -> text.toFloatOrNull() ?: 0f
        "double" -> text.toDoubleOrNull() ?: 0.0
        "Vector3" -> parseVector3(text)
        "Color3" -> parseColor3(text)
        "UDim2" -> parseUdim2(text)
        "enum" -> {
            val found = Enums.lookup(enumType ?: "", text)
            found ?: EnumValue(enumType ?: "Enum", text, 0)
        }
        else -> text
    }

    private fun parseVector3(text: String): Vector3 {
        val values = floats(text, 3)
        return Vector3(values[0], values[1], values[2])
    }

    private fun parseColor3(text: String): Color3 {
        val values = floats(text, 3)
        return Color3(values[0], values[1], values[2])
    }

    private fun parseCFrame(text: String): CFrame {
        val v = floats(text, 12)
        return CFrame(
            v[0], v[1], v[2],
            v[3], v[4], v[5],
            v[6], v[7], v[8],
            v[9], v[10], v[11]
        )
    }

    private fun parseUdim2(text: String): UDim2 {
        val v = floats(text, 4)
        return UDim2.new(v[0], v[1], v[2], v[3])
    }

    private fun floats(text: String, count: Int): FloatArray {
        val parts = text.split(',').map { it.trim().toFloatOrNull() ?: 0f }
        return FloatArray(count) { parts.getOrElse(it) { 0f } }
    }

    private fun vec(value: Vector3): String = "${value.x},${value.y},${value.z}"

    private fun color(value: Color3): String =
        "${value.r},${value.g},${value.b}"

    private fun frame(value: CFrame): String = listOf(
        value.x, value.y, value.z,
        value.r00, value.r01, value.r02,
        value.r10, value.r11, value.r12,
        value.r20, value.r21, value.r22
    ).joinToString(",")

    private fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
