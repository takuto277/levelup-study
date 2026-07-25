package org.example.project.core.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.CFRelease
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
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlocked
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS 実装: Keychain に Guest Session を保存する。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureSessionStore {

    private val service: String
        get() = "${bundleIdentifier()}.levelup.secure-session"

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        val data = json.toNSData() ?: throw IllegalStateException("Failed to encode session data")

        remove(environmentKey)

        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to keyFor(environmentKey),
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlocked,
        )

        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        val status = SecItemAdd(cfQuery, null)
        CFRelease(cfQuery)
        if (status != errSecSuccess) {
            throw IllegalStateException("Keychain save failed: $status")
        }
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to keyFor(environmentKey),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        )

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
            val status = SecItemCopyMatching(cfQuery, result.ptr)
            CFRelease(cfQuery)
            if (status != errSecSuccess) return null

            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            val json = data.toKotlinString() ?: return null
            return Json.decodeFromString(json)
        }
    }

    actual suspend fun remove(environmentKey: String) {
        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to keyFor(environmentKey),
        )

        val cfQuery = CFBridgingRetain(query) as CFDictionaryRef
        SecItemDelete(cfQuery)
        CFRelease(cfQuery)
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
