package org.example.project.core.storage

import android.content.Context
import android.content.SharedPreferences

private lateinit var appContext: Context

fun initKeyValueStore(context: Context) {
    appContext = context.applicationContext
}

/** Connectivity など共有モジュールから参照 */
internal fun requireAndroidAppContext(): Context = appContext

actual class KeyValueStore actual constructor() : KeyValueStorage {
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("levelup_prefs", Context.MODE_PRIVATE)
    }

    actual override fun getString(key: String): String? = prefs.getString(key, null)

    actual override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual override fun clear() {
        prefs.edit().clear().apply()
    }
}
