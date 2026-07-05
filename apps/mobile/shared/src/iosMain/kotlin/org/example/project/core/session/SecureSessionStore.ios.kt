package org.example.project.core.session

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
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
 *
 * service は Bundle ID ベースの固定値、account は環境別キーとする。
 * 平文フォールバックは行わない。
 */
@OptIn(ExperimentalForeignApi::class)
actual class SecureSessionStore {

    private val service: String
        get() = "${bundleIdentifier()}.levelup.secure-session"

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        val data = json.toNSData() ?: throw IllegalStateException("Failed to encode session data")
        val account = keyFor(environmentKey)

        // 既存を削除してから追加（更新の複雑さを避ける）
        remove(environmentKey)

        val query = NSMutableDictionary().apply {
            setValue(kSecClassGenericPassword, forKey = kSecClass)
            setValue(service, forKey = kSecAttrService)
            setValue(account, forKey = kSecAttrAccount)
            setValue(data, forKey = kSecValueData)
        }

        val status = SecItemAdd(query, null)
        if (status != errSecSuccess) {
            throw IllegalStateException("Keychain save failed: $status")
        }
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        val query = NSMutableDictionary().apply {
            setValue(kSecClassGenericPassword, forKey = kSecClass)
            setValue(service, forKey = kSecAttrService)
            setValue(keyFor(environmentKey), forKey = kSecAttrAccount)
            setValue(true, forKey = kSecReturnData)
            setValue(kSecMatchLimitOne, forKey = kSecMatchLimit)
        }

        memScoped {
            val result = allocArrayOf<Any?>(null)
            val status = SecItemCopyMatching(query, result)
            if (status != errSecSuccess) return null

            val data = result[0] as? NSData ?: return null
            val json = data.toKotlinString() ?: return null
            return Json.decodeFromString(json)
        }
    }

    actual suspend fun remove(environmentKey: String) {
        val query = NSMutableDictionary().apply {
            setValue(kSecClassGenericPassword, forKey = kSecClass)
            setValue(service, forKey = kSecAttrService)
            setValue(keyFor(environmentKey), forKey = kSecAttrAccount)
        }
        SecItemDelete(query)
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
