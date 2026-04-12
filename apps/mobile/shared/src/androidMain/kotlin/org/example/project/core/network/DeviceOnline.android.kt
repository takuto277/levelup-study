package org.example.project.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.example.project.core.storage.requireAndroidAppContext

actual fun isDeviceOnline(): Boolean {
    return try {
        val ctx: Context = requireAndroidAppContext()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}
