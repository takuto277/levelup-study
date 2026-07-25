package org.example.project.core.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFBooleanTrue
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
        val dictRef = CFBridgingRetain(nsData)
        try {
            remove(environmentKey)
            val query = CFBridgingRetain(mapOf<Any, Any>(
                kSecClass as Any to kSecClassGenericPassword as Any,
                kSecAttrService as Any to service as Any,
                kSecAttrAccount as Any to keyFor(environmentKey) as Any,
                kSecValueData as Any to dictRef as Any,
            )) as CFDictionaryRef
            val status = SecItemAdd(query, null)
            CFRelease(query)
            println("[SecureSessionStore] SecItemAdd status=$status service=$service account=${keyFor(environmentKey)}")
            if (status != errSecSuccess) {
                throw IllegalStateException("Keychain save failed: $status")
            }
        } finally {
            CFRelease(dictRef)
        }
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val query = CFBridgingRetain(mapOf<Any, Any>(
                kSecClass as Any to kSecClassGenericPassword as Any,
                kSecAttrService as Any to service as Any,
                kSecAttrAccount as Any to keyFor(environmentKey) as Any,
                kSecReturnData as Any to kCFBooleanTrue as Any,
                kSecMatchLimit as Any to kSecMatchLimitOne as Any,
            )) as CFDictionaryRef
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)
            if (status != errSecSuccess) return null

            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            val json = data.toKotlinString() ?: return null
            return Json.decodeFromString(json)
        }
    }

    actual suspend fun remove(environmentKey: String) {
        val query = CFBridgingRetain(mapOf<Any, Any>(
            kSecClass as Any to kSecClassGenericPassword as Any,
            kSecAttrService as Any to service as Any,
            kSecAttrAccount as Any to keyFor(environmentKey) as Any,
        )) as CFDictionaryRef
        val status = SecItemDelete(query)
        CFRelease(query)
        println("[SecureSessionStore] SecItemDelete status=$status")
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
