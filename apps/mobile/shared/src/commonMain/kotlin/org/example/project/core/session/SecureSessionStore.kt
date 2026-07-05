package org.example.project.core.session

/**
 * Guest Session を安全なプラットフォームストレージに保存する expect/actual。
 *
 * - Android: Android Keystore で暗号化し専用 SharedPreferences/DataStore へ保存
 * - iOS: Keychain へ保存
 *
 * 保存に失敗した場合は平文フォールバックせず、例外を投げる。
 */
expect class SecureSessionStore {

    /**
     * 指定した環境キーで [StoredGuestSession] を保存する。
     */
    suspend fun save(environmentKey: String, session: StoredGuestSession)

    /**
     * 指定した環境キーの [StoredGuestSession] を読み込む。
     * 未保存の場合は null を返す。
     */
    suspend fun load(environmentKey: String): StoredGuestSession?

    /**
     * 指定した環境キーの Guest Session を削除する。
     */
    suspend fun remove(environmentKey: String)
}
