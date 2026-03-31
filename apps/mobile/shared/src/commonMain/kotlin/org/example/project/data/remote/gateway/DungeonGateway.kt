package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.core.session.UserSessionStore
import org.example.project.data.remote.dto.DungeonProgressListResponse
import org.example.project.data.remote.dto.DungeonStageListResponse
import org.example.project.data.remote.dto.MasterDungeonListResponse

/**
 * ダンジョン API Gateway
 * マスタデータ・ステージ・進行状況の取得
 */
class DungeonGateway(private val client: HttpClient) {

    /** GET /api/v1/master/dungeons — ダンジョンマスタ一覧 */
    suspend fun getDungeons(): NetworkResult<MasterDungeonListResponse> = runCatching {
        val response: MasterDungeonListResponse =
            client.get(ApiRoutes.MASTER_DUNGEONS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ダンジョン情報の取得に失敗しました")
    }

    /** GET /api/v1/master/dungeons/{dungeonId} — ダンジョン詳細（ステージ含む） */
    suspend fun getDungeonStages(dungeonId: String): NetworkResult<DungeonStageListResponse> =
        runCatching {
            val response: DungeonStageListResponse =
                client.get(ApiRoutes.masterDungeon(dungeonId)).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ステージ情報の取得に失敗しました")
        }

    /** GET /api/v1/users/{userId}/dungeons — 全ダンジョン進行状況 */
    suspend fun getAllProgress(): NetworkResult<DungeonProgressListResponse> = runCatching {
        val userId = UserSessionStore.requireUserId()
        val response: DungeonProgressListResponse =
            client.get(ApiRoutes.dungeonProgress(userId)).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ダンジョン進行状況の取得に失敗しました")
    }

    /** GET /api/v1/users/{userId}/dungeons?dungeon_id={id} — 特定ダンジョンの進行状況 */
    suspend fun getProgress(dungeonId: String): NetworkResult<DungeonProgressListResponse> =
        runCatching {
            val userId = UserSessionStore.requireUserId()
            val response: DungeonProgressListResponse =
                client.get(ApiRoutes.dungeonProgress(userId)) {
                    parameter("dungeon_id", dungeonId)
                }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ダンジョン進行状況の取得に失敗しました")
        }
}
