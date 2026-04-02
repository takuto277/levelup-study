package org.example.project.core.storage

/**
 * プラットフォーム固有の Key-Value ストア。
 * iOS: NSUserDefaults / Android: SharedPreferences
 */
expect class KeyValueStore() {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}
