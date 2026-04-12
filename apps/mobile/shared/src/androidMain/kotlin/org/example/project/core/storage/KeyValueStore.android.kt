package org.example.project.core.storage

import android.content.Context
import android.content.SharedPreferences

private lateinit var appContext: Context

fun initKeyValueStore(context: Context) {
    appContext = context.applicationContext
}

/** Connectivity など共有モジュールから参照 */
internal fun requireAndroidAppContext(): Context = appContext

actual class KeyValueStore actual constructor() {
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("levelup_prefs", Context.MODE_PRIVATE)
    }

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }
}
