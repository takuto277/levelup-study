package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.DungeonGateway
import org.example.project.domain.model.DungeonProgress
import org.example.project.domain.model.DungeonStage
import org.example.project.domain.model.MasterDungeon
import org.example.project.domain.repository.DungeonRepository

class DungeonRepositoryImpl(
    private val gateway: DungeonGateway
) : DungeonRepository {

    /** マスタデータのメモリキャッシュ */
    private var dungeonCache: List<MasterDungeon>? = null

    override suspend fun getDungeons(): List<MasterDungeon> {
        dungeonCache?.let { return it }
        val dungeons = gateway.getDungeons().getOrThrow()
            .dungeons.map { it.toDomain() }
        dungeonCache = dungeons
        return dungeons
    }

    override suspend fun getDungeonStages(dungeonId: String): List<DungeonStage> {
        return gateway.getDungeonStages(dungeonId).getOrThrow()
            .stages.map { it.toDomain() }
    }

    override suspend fun getAllProgress(): List<DungeonProgress> {
        return gateway.getAllProgress().getOrThrow()
            .progress.map { it.toDomain() }
    }

    override suspend fun getProgress(dungeonId: String): DungeonProgress? {
        return gateway.getProgress(dungeonId).getOrThrow()
            .progress.map { it.toDomain() }
            .firstOrNull()
    }
}
