package com.tetrazbloxy.mobilestudio.model

import java.util.concurrent.CopyOnWriteArrayList

/** Receives tree mutation events from a [DataModel] root. */
interface TreeListener {
    fun onDescendantAdded(parent: Instance, child: Instance)
    fun onDescendantRemoving(parent: Instance, child: Instance)
    fun onPropertyChanged(instance: Instance, property: String)
}

/**
 * Roblox-like instance tree node. Property values are intentionally untyped
 * (like the original engine's property bag) while [className] drives ClassDb
 * inheritance checks.
 */
open class Instance(
    classLabel: String = "Instance",
    initialName: String = classLabel
) {
    open val className: String = classLabel

    var name: String = initialName
        set(value) {
            if (field == value) return
            field = value
            changed(PropNames.NAME)
        }

    var parent: Instance? = null
        set(value) {
            if (field === value) return
            val previous = field
            previous?.childList?.remove(this)
            field = value
            value?.childList?.add(this)
            changed(PropNames.PARENT)
        }

    private val childList = mutableListOf<Instance>()
    private val propertyValues = LinkedHashMap<String, Any?>()
    private val changeListeners = CopyOnWriteArrayList<(Instance, String) -> Unit>()

    val children: List<Instance> get() = childList

    fun getChildren(): List<Instance> = childList.toList()

    fun addChild(child: Instance): Instance {
        child.parent = this
        return child
    }

    fun removeChild(child: Instance) {
        if (child.parent === this) {
            child.parent = null
        }
    }

    fun clearChildren() {
        for (child in childList.toList()) {
            child.parent = null
        }
    }

    fun destroy() {
        clearChildren()
        parent = null
        changeListeners.clear()
    }

    fun findFirstChild(name: String, recursive: Boolean = false): Instance? {
        for (child in childList) {
            if (child.name == name) return child
        }
        if (recursive) {
            for (child in childList) {
                val found = child.findFirstChild(name, true)
                if (found != null) return found
            }
        }
        return null
    }

    fun findFirstChildOfClass(className: String): Instance? =
        childList.firstOrNull { it.className == className }

    fun findFirstChildWhichIsA(className: String, recursive: Boolean = false): Instance? {
        for (child in childList) {
            if (child.isA(className)) return child
        }
        if (recursive) {
            for (child in childList) {
                val found = child.findFirstChildWhichIsA(className, true)
                if (found != null) return found
            }
        }
        return null
    }

    fun getDescendants(): List<Instance> {
        val result = mutableListOf<Instance>()
        fun visit(node: Instance) {
            for (child in node.childList) {
                result.add(child)
                visit(child)
            }
        }
        visit(this)
        return result
    }

    fun isDescendantOf(ancestor: Instance): Boolean {
        var current = parent
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }

    fun isAncestorOf(descendant: Instance): Boolean = descendant.isDescendantOf(this)

    fun isA(targetClassName: String): Boolean = ClassDb.isA(className, targetClassName)

    fun getFullName(): String {
        val parts = mutableListOf<String>()
        var current: Instance? = this
        while (current != null) {
            parts.add(current.name)
            current = current.parent
        }
        return parts.asReversed().joinToString(".")
    }

    open fun getProperty(key: String): Any? = when (key) {
        PropNames.NAME -> name
        PropNames.CLASS_NAME -> className
        PropNames.PARENT -> parent
        else -> propertyValues[key]
    }

    open fun setProperty(key: String, value: Any?): Boolean = when (key) {
        PropNames.NAME -> {
            name = value?.toString() ?: ""
            true
        }

        PropNames.CLASS_NAME -> false

        PropNames.PARENT -> {
            parent = value as? Instance
            true
        }

        else -> {
            propertyValues[key] = value
            changed(key)
            true
        }
    }

    fun hasProperty(key: String): Boolean = when (key) {
        PropNames.NAME, PropNames.CLASS_NAME, PropNames.PARENT -> true
        else -> propertyValues.containsKey(key)
    }

    fun removeProperty(key: String): Boolean = propertyValues.remove(key) != null

    fun getProperties(): Map<String, Any?> = LinkedHashMap(propertyValues)

    open fun changed(property: String) {
        for (listener in changeListeners) {
            listener(this, property)
        }
    }

    /** Registers a change listener and returns a disconnect function. */
    fun onChanged(listener: (Instance, String) -> Unit): () -> Unit {
        changeListeners.add(listener)
        return { changeListeners.remove(listener) }
    }

    open fun clone(): Instance {
        val copy = InstanceFactory.create(className)
        copy.name = name
        copyStateTo(copy)
        for ((key, value) in propertyValues) {
            copy.propertyValues[key] = value
        }
        for (child in childList) {
            val childCopy = child.clone()
            childCopy.parent = copy
        }
        return copy
    }

    /** Copies fields declared by subclasses; overridden by typed instances. */
    protected open fun copyStateTo(target: Instance) = Unit

    override fun toString(): String = "$className($name)"
}
