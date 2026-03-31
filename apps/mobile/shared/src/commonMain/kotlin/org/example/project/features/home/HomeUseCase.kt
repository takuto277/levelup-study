package org.example.project.features.home

import org.example.project.core.session.UserSessionStore
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

    /**
     * ホーム画面に必要な全データを一括取得
     *
     * 初回起動時はユーザーが未作成なので、自動作成する。
     * UserSessionStore に userId がセットされていれば既存ユーザーとして取得。
     */
    suspend fun loadHomeData(): HomeData {
        // ユーザーがまだ存在しなければ作成
        val user = ensureUser()

        // パーティ情報を取得（未編成でも OK）
        val mainCharacter: UserCharacter? = try {
            val party = partyRepository.getParty()
            party.mainCharacter
                ?: party.slots.firstOrNull()?.userCharacterId?.let { id ->
                    characterRepository.getUserCharacter(id)
                }
        } catch (_: Exception) {
            null // パーティ未編成
        }

        return HomeData(user = user, mainCharacter = mainCharacter)
    }

    /** サーバーから最新のユーザー情報を再取得 */
    suspend fun refreshUser(): User {
        return userRepository.syncFromServer()
    }

    /**
     * ユーザーが存在することを保証する
     * セッションに userId がなければ新規作成
     */
    private suspend fun ensureUser(): User {
        return if (UserSessionStore.userId != null) {
            userRepository.getCurrentUser()
        } else {
            // 初回起動 → 自動でユーザー作成
            userRepository.createUser("冒険者")
        }
    }
}
