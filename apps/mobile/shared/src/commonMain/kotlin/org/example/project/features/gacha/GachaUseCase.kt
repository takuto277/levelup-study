package org.example.project.features.gacha

import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResult
import org.example.project.domain.repository.GachaRepository
import org.example.project.domain.repository.UserRepository

/**
 * 召喚画面のユースケース
 * バナー取得・ガチャ実行
 */
class GachaUseCase(
    private val gachaRepository: GachaRepository,
    private val userRepository: UserRepository
) {
    /** バナー一覧を取得 */
    suspend fun loadBanners(): List<GachaBanner> {
        return gachaRepository.getActiveBanners()
    }

    /** 現在の石数を取得 */
    suspend fun getCurrentStones(): Int {
        return userRepository.getCurrentUser().stones
    }

    /** ガチャを実行（サーバー側で確率計算） */
    suspend fun pullGacha(bannerId: String, count: Int): List<GachaResult> {
        return gachaRepository.pullGacha(bannerId, count)
    }
}
