package org.example.project.core.network

/**
 * iOS actual. KMP-interop for SCNetworkReachability/NWPathMonitor is fragile.
 * Pending sync queue handles offline gracefully (send fails → saved locally).
 * TODO: implement with Swift-KMP bridge when interop stabilizes.
 */
actual fun isDeviceOnline(): Boolean = true
