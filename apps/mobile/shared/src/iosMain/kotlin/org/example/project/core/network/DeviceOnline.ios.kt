package org.example.project.core.network

/**
 * iOS actual. NWPathMonitor is available via Utils/NetworkMonitor.swift.
 * KMP direct interop with valueForKey is unstable; use @objc bridge when interop stabilizes.
 * Pending sync queue handles offline gracefully.
 */
actual fun isDeviceOnline(): Boolean = true
