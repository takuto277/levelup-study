package org.example.project.core.session

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.core.storage.requireAndroidAppContext
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android 実装: Android Keystore + AES-GCM + 専用 SharedPreferences。
 *
 * 暗号化キーは Keystore に保存され、extractable=false。
 * 平文フォールバックは行わない。
 */
actual class SecureSessionStore {

    private val prefs: SharedPreferences by lazy {
        requireAndroidAppContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    actual suspend fun save(environmentKey: String, session: StoredGuestSession) {
        val json = Json.encodeToString(session)
        val encrypted = encrypt(json.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(keyFor(environmentKey), encode(encrypted)).apply()
    }

    actual suspend fun load(environmentKey: String): StoredGuestSession? {
        val encoded = prefs.getString(keyFor(environmentKey), null) ?: return null
        val bytes = decrypt(decode(encoded))
        val json = String(bytes, Charsets.UTF_8)
        return Json.decodeFromString(json)
    }

    actual suspend fun remove(environmentKey: String) {
        prefs.edit().remove(keyFor(environmentKey)).apply()
    }

    private fun keyFor(environmentKey: String): String = "guest_session_$environmentKey"

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encrypted.copyOfRange(GCM_IV_LENGTH, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "levelup_guest_session_key"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE = 256
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val PREFS_NAME = "levelup_secure_session"
    }
}
