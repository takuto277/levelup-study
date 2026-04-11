package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import org.example.project.core.network.ApiClient
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

    suspend fun deleteGenre(genreId: String): NetworkResult<Unit> =
        runCatching {
            val response: HttpResponse = client.delete(ApiRoutes.masterGenre(genreId))
            val code = response.status.value
            if (code in 200..299) {
                NetworkResult.Success(Unit)
            } else {
                val raw = response.bodyAsText()
                val msg = parseApiErrorMessage(raw)
                    ?: "ジャンル削除に失敗しました (HTTP $code)"
                NetworkResult.Error(code = code, message = msg)
            }
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ジャンル削除に失敗しました")
        }
}

private fun parseApiErrorMessage(json: String): String? =
    runCatching {
        ApiClient.json.decodeFromString<ApiErrorEnvelope>(json).error
    }.getOrNull()?.takeIf { it.isNotBlank() }

@kotlinx.serialization.Serializable
private data class ApiErrorEnvelope(val error: String = "")

@kotlinx.serialization.Serializable
data class CreateGenreRequest(
    val slug: String,
    val label: String,
    val emoji: String = "\uD83D\uDCD6",
    val color_hex: String = "#6B7280"
)
