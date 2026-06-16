package org.example.project.core.network

import kotlin.concurrent.Volatile

@Volatile
var iosNetworkMonitorOnline: Boolean = true

actual fun isDeviceOnline(): Boolean = iosNetworkMonitorOnline
