package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.GenreListResponse

class GenreGateway(private val client: HttpClient) {

    suspend fun listGenres(): NetworkResult<GenreListResponse> =
        runCatching {
            val response: GenreListResponse = client.get(ApiRoutes.MASTER_GENRES).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ジャンル一覧の取得に失敗しました")
        }
}
