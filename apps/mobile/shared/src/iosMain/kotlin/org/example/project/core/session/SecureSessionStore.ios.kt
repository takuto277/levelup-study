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
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSMutableDictionary
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

    init {
        println("[SecureSessionStore] Init: kSecClass=$kSecClass kSecClassGenericPassword=$kSecClassGenericPassword kSecAttrService=$kSecAttrService kSecAttrAccount=$kSecAttrAccount kSecValueData=$kSecValueData")
    }

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        val data = json.toNSData() ?: throw IllegalStateException("Failed to encode session data")

        remove(environmentKey)

        @Suppress("UNCHECKED_CAST")
        val dict = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass as NSCopyingProtocol)
            setObject(service, kSecAttrService as NSCopyingProtocol)
            setObject(keyFor(environmentKey), kSecAttrAccount as NSCopyingProtocol)
            setObject(data, kSecValueData as NSCopyingProtocol)
        }
        val cfDict = CFBridgingRetain(dict) as CFDictionaryRef
        val status = SecItemAdd(cfDict, null)
        CFRelease(cfDict)
        println("[SecureSessionStore] SecItemAdd status=$status")
        if (status != errSecSuccess) {
            throw IllegalStateException("Keychain save failed: $status")
        }
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        @Suppress("UNCHECKED_CAST")
        val dict = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass as NSCopyingProtocol)
            setObject(service, kSecAttrService as NSCopyingProtocol)
            setObject(keyFor(environmentKey), kSecAttrAccount as NSCopyingProtocol)
            setObject(kCFBooleanTrue, kSecReturnData as NSCopyingProtocol)
            setObject(kSecMatchLimitOne, kSecMatchLimit as NSCopyingProtocol)
        }

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val cfDict = CFBridgingRetain(dict) as CFDictionaryRef
            val status = SecItemCopyMatching(cfDict, result.ptr)
            CFRelease(cfDict)
            if (status != errSecSuccess) return null

            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            val json = data.toKotlinString() ?: return null
            return Json.decodeFromString(json)
        }
    }

    actual suspend fun remove(environmentKey: String) {
        @Suppress("UNCHECKED_CAST")
        val dict = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass as NSCopyingProtocol)
            setObject(service, kSecAttrService as NSCopyingProtocol)
            setObject(keyFor(environmentKey), kSecAttrAccount as NSCopyingProtocol)
        }
        val cfDict = CFBridgingRetain(dict) as CFDictionaryRef
        val status = SecItemDelete(cfDict)
        CFRelease(cfDict)
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
