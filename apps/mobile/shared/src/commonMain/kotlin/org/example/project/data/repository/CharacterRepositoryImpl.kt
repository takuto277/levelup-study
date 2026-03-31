package org.example.project.data.repository

import org.example.project.core.network.getOrThrow
import org.example.project.data.remote.dto.toDomain
import org.example.project.data.remote.gateway.CharacterGateway
import org.example.project.domain.model.MasterCharacter
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.repository.CharacterRepository

class CharacterRepositoryImpl(
    private val gateway: CharacterGateway
) : CharacterRepository {

    /** マスタデータのメモリキャッシュ */
    private var masterCache: List<MasterCharacter>? = null

    override suspend fun getMasterCharacters(): List<MasterCharacter> {
        masterCache?.let { return it }
        val characters = gateway.getMasterCharacters().getOrThrow()
            .characters.map { it.toDomain() }
        masterCache = characters
        return characters
    }

    override suspend fun getUserCharacters(): List<UserCharacter> {
        return gateway.getUserCharacters().getOrThrow()
            .characters.map { it.toDomain() }
    }

    override suspend fun getUserCharacter(id: String): UserCharacter? {
        return getUserCharacters().find { it.id == id }
    }

    override suspend fun levelUpCharacter(userCharacterId: String): UserCharacter {
        TODO("レベルアップ API は未実装です")
    }
}
