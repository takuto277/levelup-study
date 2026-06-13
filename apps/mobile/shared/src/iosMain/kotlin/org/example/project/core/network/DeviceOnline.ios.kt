package org.example.project.core.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStreamCreateWithBSDSocket
import platform.Foundation.CFBridgingRelease
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.darwin.UInt8Var
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import kotlinx.cinterop.ByteVar

@OptIn(ExperimentalForeignApi::class)
actual fun isDeviceOnline(): Boolean {
    return try {
        memScoped {
            val addr = alloc<platform.posix.sockaddr_in>()
            addr.sin_family = AF_INET.toShort()
            addr.sin_len = 16.toUByte()
            addr.sin_addr.s_addr = platform.posix.inet_addr("8.8.8.8")

            val reachability = SCNetworkReachabilityCreateWithAddress(null, addr.ptr)
            if (reachability == null) return true

            val flags = memScoped {
                val flagsPtr = alloc<SCNetworkReachabilityFlags>()
                val ok = SCNetworkReachabilityGetFlags(reachability, flagsPtr.ptr)
                if (ok) flagsPtr.value else 0u
            }
            CFRelease(reachability)

            val reachable = (flags and 2u) != 0u // kSCNetworkReachabilityFlagsReachable
            reachable
        }
    } catch (e: Exception) {
        true // default to online to avoid blocking pending syncs
    }
}
