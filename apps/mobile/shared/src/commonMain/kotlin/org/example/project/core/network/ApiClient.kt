package org.example.project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.core.session.GuestAuthService
import org.example.project.core.session.SecureSessionStore

/**
 * Ktor HttpClient ファクトリ
 * アプリ全体で共有する単一の HttpClient インスタンスを生成
 */
object ApiClient {

    /**
     * JSON パーサー設定
     * - 未知のキーを無視（サーバー側のレスポンスに新フィールドが追加されても壊れない）
     * - null 許容フィールドの省略を許可
     */
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    /**
     * HttpClient を生成
     * Ktor のエンジンは各プラットフォーム (OkHttp / Darwin) で自動解決される。
     *
     * @param isDebug デバッグビルド時は HTTP ボディログを有効化。Release では無効化し、
     *                いずれの場合も Authorization ヘッダーはマスクする。
     */
    fun create(
        isDebug: Boolean,
        guestAuthService: GuestAuthService,
        secureSessionStore: SecureSessionStore,
    ): HttpClient {
        return HttpClient {
            // JSON シリアライゼーション
            install(ContentNegotiation) {
                json(json)
            }

            // ロギング（Debug のみ有効、Authorization は常にマスク）
            install(Logging) {
                level = if (isDebug) LogLevel.BODY else LogLevel.NONE
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }

            // Guest モード時の access token 期限切れ refresh
            TokenRefreshInterceptor(guestAuthService, secureSessionStore).install(this)

            // デフォルトリクエスト設定
            defaultRequest {
                url(ApiRoutes.BASE_URL)
                contentType(ContentType.Application.Json)
                if (GENERATED_CLIENT_API_KEY.isNotEmpty()) {
                    header("X-API-Key", GENERATED_CLIENT_API_KEY)
                }
                val bearer = AuthTokenProvider.currentToken()
                bearer?.let { header("Authorization", "Bearer $it") }
            }
        }
    }
}
