package org.example.project.features.home

import org.example.project.domain.model.User
import org.example.project.domain.model.UserCharacter
import org.example.project.domain.repository.CharacterRepository
import org.example.project.domain.repository.PartyRepository
import org.example.project.domain.repository.UserRepository

/**
 * ホーム画面のユースケース
 * ユーザーステータス + メインキャラクター表示に必要なデータを集約
 */
class HomeUseCase(
    private val userRepository: UserRepository,
    private val partyRepository: PartyRepository,
    private val characterRepository: CharacterRepository
) {
    /** ホーム画面データ */
    data class HomeData(
        val user: User,
        val mainCharacter: UserCharacter?
    )

    /** ホーム画面に必要な全データを一括取得 */
    suspend fun loadHomeData(): HomeData {
        val user = userRepository.getCurrentUser()
        val party = runCatching { partyRepository.getParty() }.getOrNull()
        val mainCharacter = party?.mainCharacter
            ?: party?.slots?.firstOrNull()?.userCharacterId?.let { id ->
                characterRepository.getUserCharacter(id)
            }

        return HomeData(user = user, mainCharacter = mainCharacter)
    }

    /** サーバーから最新のユーザー情報を再取得 */
    suspend fun refreshUser(): User {
        return userRepository.syncFromServer()
    }
}
