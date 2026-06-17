package org.example.project.core.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.posix.AF_INET
import platform.posix.sockaddr_in

/**
 * NWPathMonitor から Swift 側がリアルタイムに更新するオンライン状態。
 * KMP → Swift export により `DeviceOnline_iosKt.iosNetworkMonitorOnline` でアクセス可能。
 */
var iosNetworkMonitorOnline: Boolean = true

/**
 * iOS actual. NWPathMonitor のリアルタイム値を返す。
 * Swift 側で NetworkMonitor が未起動の場合は SCNetworkReachability でフォールバック判定する。
 */
@OptIn(ExperimentalForeignApi::class)
actual fun isDeviceOnline(): Boolean = if (!iosNetworkMonitorOnline) false else memScoped {
    val zeroAddress = alloc<sockaddr_in>().apply {
        sin_len = sizeOf<sockaddr_in>().toUByte()
        sin_family = AF_INET.toUByte()
    }
    val reachability = SCNetworkReachabilityCreateWithAddress(null, zeroAddress.ptr.reinterpret())
        ?: return@memScoped false

    val flags = alloc<SCNetworkReachabilityFlagsVar>()
    if (!SCNetworkReachabilityGetFlags(reachability, flags.ptr)) {
        return@memScoped false
    }

    val isReachable = (flags.value and kSCNetworkReachabilityFlagsReachable) != 0u
    val connectionRequired = (flags.value and kSCNetworkReachabilityFlagsConnectionRequired) != 0u
    isReachable && !connectionRequired
}
