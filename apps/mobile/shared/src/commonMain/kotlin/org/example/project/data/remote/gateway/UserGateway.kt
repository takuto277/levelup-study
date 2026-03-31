package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.CreateUserRequest
import org.example.project.data.remote.dto.UpdateUserRequest
import org.example.project.data.remote.dto.UserResponse

/**
 * ユーザー API Gateway
 * Go バックエンドの user 系エンドポイントとの通信
 */
class UserGateway(private val client: HttpClient) {

    /** POST /api/v1/users — ユーザー作成 */
    suspend fun createUser(request: CreateUserRequest): NetworkResult<UserResponse> = runCatching {
        val response: UserResponse = client.post(ApiRoutes.USERS) {
            setBody(request)
        }.body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ユーザー作成に失敗しました")
    }

    /** GET /api/v1/users/{userId} */
    suspend fun getUser(userId: String): NetworkResult<UserResponse> = runCatching {
        val response: UserResponse = client.get(ApiRoutes.user(userId)).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ユーザー情報の取得に失敗しました")
    }

    /** PUT /api/v1/users/{userId} */
    suspend fun updateUser(userId: String, request: UpdateUserRequest): NetworkResult<UserResponse> = runCatching {
        val response: UserResponse = client.put(ApiRoutes.user(userId)) {
            setBody(request)
        }.body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ユーザー情報の更新に失敗しました")
    }
}
