package org.example.project.core.storage

import platform.Foundation.NSUserDefaults

actual class KeyValueStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? =
        defaults.stringForKey(key)

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    actual fun clear() {
        val dict = defaults.dictionaryRepresentation()
        for (key in dict.keys) {
            (key as? String)?.let { defaults.removeObjectForKey(it) }
        }
    }
}
