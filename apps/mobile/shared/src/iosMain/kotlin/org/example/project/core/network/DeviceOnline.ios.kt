package org.example.project.core.network

import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.CoreFoundation.CFRelease
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr

@OptIn(ExperimentalForeignApi::class)
actual fun isDeviceOnline(): Boolean {
    return try {
        memScoped {
            val reachability = SCNetworkReachabilityCreateWithName(null, "8.8.8.8")
            if (reachability == null) return true

            val flags = alloc<UIntVar>()
            val ok = SCNetworkReachabilityGetFlags(reachability, flags.ptr)
            CFRelease(reachability)

            if (ok) {
                (flags.value.toLong() and kSCNetworkReachabilityFlagsReachable.toLong()) != 0L
            } else {
                true
            }
        }
    } catch (e: Exception) {
        true
    }
}
