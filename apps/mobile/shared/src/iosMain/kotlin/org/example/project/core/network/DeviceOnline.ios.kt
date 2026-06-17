package org.example.project.core.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.posix.AF_INET
import platform.posix.sockaddr_in

/**
 * iOS actual. SCNetworkReachability でインターネット接続の可否を判定する。
 * ゼロアドレス（0.0.0.0）を使い、一般のインターネット到達性を確認する。
 */
@OptIn(ExperimentalForeignApi::class)
actual fun isDeviceOnline(): Boolean = memScoped {
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
