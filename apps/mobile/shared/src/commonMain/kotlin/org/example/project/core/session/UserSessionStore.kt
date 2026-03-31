package org.example.project.core.session

/**
 * ユーザーセッション管理
 *
 * アプリ起動時にユーザーID を保持し、全 API コールで利用する。
 * 現在はメモリ上のみ。将来的には Multiplatform Settings で永続化する。
 */
object UserSessionStore {

    /** 現在ログイン中のユーザー ID */
    var userId: String? = null
        private set

    /** 認証トークン (JWT) — 将来 Supabase Auth 連携時に使用 */
    var authToken: String? = null
        private set

    /** ユーザーセッションを設定 */
    fun setSession(userId: String, token: String? = null) {
        this.userId = userId
        this.authToken = token
    }

    /** セッションをクリア（ログアウト時） */
    fun clear() {
        userId = null
        authToken = null
    }

    /** ユーザーIDを取得（未設定の場合は例外） */
    fun requireUserId(): String =
        userId ?: throw IllegalStateException("ユーザーIDが設定されていません。先にログインしてください。")
}
