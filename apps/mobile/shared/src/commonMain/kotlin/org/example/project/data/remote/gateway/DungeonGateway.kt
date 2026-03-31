package org.example.project.data.remote.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.example.project.core.network.ApiRoutes
import org.example.project.core.network.NetworkResult
import org.example.project.data.remote.dto.DungeonProgressListResponse
import org.example.project.data.remote.dto.DungeonStageListResponse
import org.example.project.data.remote.dto.MasterDungeonListResponse

/**
 * ダンジョン API Gateway
 * マスタデータ・ステージ・進行状況の取得
 */
class DungeonGateway(private val client: HttpClient) {

    /** GET /api/master/dungeons — ダンジョンマスタ一覧 */
    suspend fun getDungeons(): NetworkResult<MasterDungeonListResponse> = runCatching {
        val response: MasterDungeonListResponse = client.get(ApiRoutes.DUNGEONS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ダンジョン情報の取得に失敗しました")
    }

    /** GET /api/master/dungeons/{id}/stages — ステージ一覧 */
    suspend fun getDungeonStages(dungeonId: String): NetworkResult<DungeonStageListResponse> =
        runCatching {
            val response: DungeonStageListResponse =
                client.get(ApiRoutes.dungeonStages(dungeonId)).body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ステージ情報の取得に失敗しました")
        }

    /** GET /api/user/dungeon-progress — 全ダンジョン進行状況 */
    suspend fun getAllProgress(): NetworkResult<DungeonProgressListResponse> = runCatching {
        val response: DungeonProgressListResponse =
            client.get(ApiRoutes.DUNGEON_PROGRESS).body()
        NetworkResult.Success(response)
    }.getOrElse { e ->
        NetworkResult.Error(message = e.message ?: "ダンジョン進行状況の取得に失敗しました")
    }

    /** GET /api/user/dungeon-progress?dungeon_id={id} — 特定ダンジョンの進行状況 */
    suspend fun getProgress(dungeonId: String): NetworkResult<DungeonProgressListResponse> =
        runCatching {
            val response: DungeonProgressListResponse =
                client.get(ApiRoutes.DUNGEON_PROGRESS) {
                    parameter("dungeon_id", dungeonId)
                }.body()
            NetworkResult.Success(response)
        }.getOrElse { e ->
            NetworkResult.Error(message = e.message ?: "ダンジョン進行状況の取得に失敗しました")
        }
}
