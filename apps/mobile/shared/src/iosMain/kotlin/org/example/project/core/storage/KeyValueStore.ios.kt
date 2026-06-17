package org.example.project.core.storage

import platform.Foundation.NSUserDefaults

actual class KeyValueStore actual constructor() : KeyValueStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual override fun getString(key: String): String? =
        defaults.stringForKey(key)

    actual override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    actual override fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            (key as? String)?.let { defaults.removeObjectForKey(it) }
        }
    }
}
