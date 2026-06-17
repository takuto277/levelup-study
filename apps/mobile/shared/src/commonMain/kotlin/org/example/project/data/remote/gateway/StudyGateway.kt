package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.ErrorMessage
import org.example.project.core.network.NetworkResult
import org.example.project.core.network.isDeviceOnline
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.StudyCompleteRequest
import org.example.project.data.remote.dto.StudyCompleteResponse
import org.example.project.data.remote.dto.StudySessionListResponse

class StudyGateway(private val client: HttpClient) {

    suspend fun completeSession(userId: String, request: StudyCompleteRequest): NetworkResult<StudyCompleteResponse> =
        runCatching {
            val response: StudyCompleteResponse = client.post(ApiRoutes.studyComplete(userId)) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            toNetworkError(e)
        }

    suspend fun listSessions(limit: Int = 20, offset: Int = 0): NetworkResult<StudySessionListResponse> =
        runCatching {
            val userId = UserSessionStore.requireUserId()
            val response: StudySessionListResponse =
                client.get(ApiRoutes.studySessions(userId)) {
                    parameter("limit", limit)
                    parameter("offset", offset)
                }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            toNetworkError(e)
        }

    private fun toNetworkError(e: Throwable): NetworkResult.Error {
        val statusCode = (e as? ClientRequestException)?.response?.status?.value
            ?: (e as? ServerResponseException)?.response?.status?.value
        return NetworkResult.Error(
            code = statusCode,
            message = ErrorMessage.classify(statusCode, e, isDeviceOnline())
        )
    }
}
