package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.StudyCompleteResponse

/**
 * 勉強セッション API Gateway
 * Go バックエンドの study 系エンドポイントとの通信
 */
class StudyGateway(private val client: HttpClient) {

    /** POST /api/v1/users/{userId}/study/complete — セッション完了 & 報酬計算 */
    suspend fun completeSession(userId: String, request: StudyCompleteRequest): NetworkResult<StudyCompleteResponse> =
        runCatching {
            val response: StudyCompleteResponse = client.post(ApiRoutes.studyComplete(userId)) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "勉強セッションの送信に失敗しました")
        }
}
