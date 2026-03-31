package org.example.project.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
     * Ktor のエンジンは各プラットフォーム (OkHttp / Darwin) で自動解決される
     */
    fun create(baseUrl: String = ApiRoutes.BASE_URL): HttpClient {
        return HttpClient {
            // JSON シリアライゼーション
            install(ContentNegotiation) {
                json(json)
            }

            // ロギング（デバッグ用）
            install(Logging) {
                level = LogLevel.BODY
            }

            // デフォルトリクエスト設定
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
