package org.example.project.core.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureSessionStore {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "levelup_secure_session_"

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        defaults.setObject(json, forKey = "${prefix}${environmentKey}")
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        val json = defaults.stringForKey("${prefix}${environmentKey}") ?: return null
        return runCatching { Json.decodeFromString<StoredGuestSession>(json) }.getOrNull()
    }

    actual suspend fun remove(environmentKey: String) {
        defaults.removeObjectForKey("${prefix}${environmentKey}")
    }
}
