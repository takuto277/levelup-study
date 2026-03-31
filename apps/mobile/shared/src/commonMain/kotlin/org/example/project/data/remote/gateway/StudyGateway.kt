package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.StudyCompleteResponse
import org.example.project.data.remote.dto.StudySessionListResponse

/**
 * 勉強セッション API Gateway
 * Go バックエンドの study 系エンドポイントとの通信
 */
class StudyGateway(private val client: HttpClient) {

    /** POST /api/study/complete — セッション完了 & 報酬計算 */
    suspend fun completeSession(request: StudyCompleteRequest): NetworkResult<StudyCompleteResponse> =
        runCatching {
            val response: StudyCompleteResponse = client.post(ApiRoutes.STUDY_COMPLETE) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "勉強セッションの送信に失敗しました")
        }

    /** GET /api/study/sessions — セッション履歴取得 */
    suspend fun getSessions(limit: Int, offset: Int): NetworkResult<StudySessionListResponse> =
        runCatching {
            val response: StudySessionListResponse = client.get(ApiRoutes.STUDY_SESSIONS) {
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "セッション履歴の取得に失敗しました")
        }
}
