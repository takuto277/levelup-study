package org.example.project.core.network

import org.example.project.core.session.SessionMode
import org.example.project.core.session.UserSessionStore

/**
 * API リクエストに付与する Bearer token を一元提供する。
 *
 * 優先順位:
 * 1. [UserSessionStore.authToken]（Supabase から取得した本物の access token）
 * 2. [DevJwtSelector.current]（Debug Seed モード時の開発 JWT）
 */
object AuthTokenProvider {

    /**
     * 現在アクティブな Bearer token を返す。未設定時は null。
     */
    fun currentToken(): String? {
        // Guest モードでは real auth token を優先
        if (UserSessionStore.sessionMode == SessionMode.GUEST) {
            return UserSessionStore.authToken?.takeIf { it.isNotBlank() }
        }
        // Seed モードでは real token があればそれを、なければ dev JWT
        return UserSessionStore.authToken?.takeIf { it.isNotBlank() }
            ?: DevJwtSelector.current.takeIf { it.isNotBlank() }
    }
}
