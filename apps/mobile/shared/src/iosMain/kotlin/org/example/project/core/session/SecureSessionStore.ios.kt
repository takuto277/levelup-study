package org.example.project.core.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureSessionStore {

    private val service: String
        get() = "${bundleIdentifier()}.levelup.secure-session"

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        val nsData = json.toNSData() ?: throw IllegalStateException("Failed to encode session data")
        val dataRef = CFBridgingRetain(nsData)
        val serviceRef = CFBridgingRetain(service)
        val accountRef = CFBridgingRetain(keyFor(environmentKey))

        try {
            remove(environmentKey)

            val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 4, kCFTypeDictionaryKeyCallBacks, kCFTypeDictionaryValueCallBacks)
            CFDictionaryAddValue(dict, kSecClass as CFTypeRef?, kSecClassGenericPassword as CFTypeRef?)
            CFDictionaryAddValue(dict, kSecAttrService as CFTypeRef?, serviceRef)
            CFDictionaryAddValue(dict, kSecAttrAccount as CFTypeRef?, accountRef)
            CFDictionaryAddValue(dict, kSecValueData as CFTypeRef?, dataRef)

            val status = SecItemAdd(dict, null)
            CFRelease(dict)
            println("[SecureSessionStore] SecItemAdd status=$status")
            if (status != errSecSuccess) {
                throw IllegalStateException("Keychain save failed: $status")
            }
        } finally {
            CFRelease(dataRef)
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        val serviceRef = CFBridgingRetain(service)
        val accountRef = CFBridgingRetain(keyFor(environmentKey))

        return try {
            memScoped {
                val result = alloc<CFTypeRefVar>()

                val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 5, kCFTypeDictionaryKeyCallBacks, kCFTypeDictionaryValueCallBacks)
                CFDictionaryAddValue(dict, kSecClass as CFTypeRef?, kSecClassGenericPassword as CFTypeRef?)
                CFDictionaryAddValue(dict, kSecAttrService as CFTypeRef?, serviceRef)
                CFDictionaryAddValue(dict, kSecAttrAccount as CFTypeRef?, accountRef)
                CFDictionaryAddValue(dict, kSecReturnData as CFTypeRef?, kCFBooleanTrue as CFTypeRef?)
                CFDictionaryAddValue(dict, kSecMatchLimit as CFTypeRef?, kSecMatchLimitOne as CFTypeRef?)

                val status = SecItemCopyMatching(dict, result.ptr)
                CFRelease(dict)
                if (status != errSecSuccess) null
                else {
                    val data = CFBridgingRelease(result.value) as? NSData
                    val json = data?.toKotlinString() ?: return null
                    Json.decodeFromString(json)
                }
            }
        } finally {
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }

    actual suspend fun remove(environmentKey: String) {
        val serviceRef = CFBridgingRetain(service)
        val accountRef = CFBridgingRetain(keyFor(environmentKey))

        try {
            val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 3, kCFTypeDictionaryKeyCallBacks, kCFTypeDictionaryValueCallBacks)
            CFDictionaryAddValue(dict, kSecClass as CFTypeRef?, kSecClassGenericPassword as CFTypeRef?)
            CFDictionaryAddValue(dict, kSecAttrService as CFTypeRef?, serviceRef)
            CFDictionaryAddValue(dict, kSecAttrAccount as CFTypeRef?, accountRef)

            val status = SecItemDelete(dict)
            CFRelease(dict)
            println("[SecureSessionStore] SecItemDelete status=$status")
        } finally {
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }

    private fun keyFor(environmentKey: String): String = "guest_session_$environmentKey"

    private fun bundleIdentifier(): String {
        return platform.Foundation.NSBundle.mainBundle.bundleIdentifier ?: "org.example.project"
    }

    private fun String.toNSData(): NSData? {
        return (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
    }

    private fun NSData.toKotlinString(): String? {
        return NSString.create(this, NSUTF8StringEncoding) as? String
    }
}
