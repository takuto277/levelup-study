package org.example.project.core.network

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSClassFromString

@OptIn(ExperimentalForeignApi::class)
actual fun isDeviceOnline(): Boolean {
    return try {
        val cls = NSClassFromString("NetworkMonitor")
        val shared = cls?.valueForKey("shared") as? Any
        (shared?.valueForKey("isOnline") as? Boolean) ?: true
    } catch (e: Exception) {
        true
    }
}
