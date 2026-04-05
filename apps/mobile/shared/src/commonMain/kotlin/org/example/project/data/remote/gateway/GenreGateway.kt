package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.GenreListResponse
import org.example.project.data.remote.dto.GenreResponse

class GenreGateway(private val client: HttpClient) {

    suspend fun listGenres(): NetworkResult<GenreListResponse> =
        runCatching {
            val response: GenreListResponse = client.get(ApiRoutes.MASTER_GENRES).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ジャンル一覧の取得に失敗しました")
        }

    suspend fun createGenre(request: CreateGenreRequest): NetworkResult<GenreResponse> =
        runCatching {
            val response: GenreResponse = client.post(ApiRoutes.MASTER_GENRES) {
                setBody(request)
            }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ジャンル作成に失敗しました")
        }
}

@kotlinx.serialization.Serializable
data class CreateGenreRequest(
    val slug: String,
    val label: String,
    val emoji: String = "\uD83D\uDCD6",
    val color_hex: String = "#6B7280"
)
