package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.UpdateUserRequest
import org.example.project.data.remote.dto.UserResponse

/**
 * ユーザー API Gateway
 * Go バックエンドの user 系エンドポイントとの通信
 */
class UserGateway(private val client: HttpClient) {

    /** GET /api/user/me */
    suspend fun getMe(): NetworkResult<UserResponse> = runCatching {
        val response: UserResponse = client.get(ApiRoutes.USER_ME).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ユーザー情報の取得に失敗しました")
    }

    /** PUT /api/user/me */
    suspend fun updateMe(request: UpdateUserRequest): NetworkResult<UserResponse> = runCatching {
        val response: UserResponse = client.put(ApiRoutes.USER_ME) {
            setBody(request)
        }.body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ユーザー情報の更新に失敗しました")
    }
}
