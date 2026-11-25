package com.tetrazbloxy.mobilestudio.storage

/**
 * Small key/value abstraction over SharedPreferences so that every
 * persistence class stays JVM-testable (no Android dependency).
 */
interface KeyValueStore {
    fun getString(key: String, defaultValue: String? = null): String?
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun getFloat(key: String, defaultValue: Float = 0f): Float
    fun getLong(key: String, defaultValue: Long = 0L): Long

    fun putString(key: String, value: String?)
    fun putBoolean(key: String, value: Boolean)
    fun putInt(key: String, value: Int)
    fun putFloat(key: String, value: Float)
    fun putLong(key: String, value: Long)

    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
    fun all(): Map<String, Any?>
}

/** In-memory store used by unit tests and previews. */
class InMemoryKeyValueStore(initial: Map<String, Any?> = emptyMap()) : KeyValueStore {
    private val values = LinkedHashMap<String, Any?>(initial)

    override fun getString(key: String, defaultValue: String?): String? =
        values[key] as? String ?: defaultValue

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun getInt(key: String, defaultValue: Int): Int =
        (values[key] as? Number)?.toInt() ?: defaultValue

    override fun getFloat(key: String, defaultValue: Float): Float =
        (values[key] as? Number)?.toFloat() ?: defaultValue

    override fun getLong(key: String, defaultValue: Long): Long =
        (values[key] as? Number)?.toLong() ?: defaultValue

    override fun putString(key: String, value: String?) {
        values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun clear() {
        values.clear()
    }

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun all(): Map<String, Any?> = LinkedHashMap(values)
}
