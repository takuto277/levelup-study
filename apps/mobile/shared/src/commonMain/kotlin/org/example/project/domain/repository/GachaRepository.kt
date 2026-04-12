package org.example.project.domain.repository

import org.example.project.domain.model.GachaBanner
import org.example.project.domain.model.GachaResult

/**
 * ガチャリポジトリ
 * バナー取得・ガチャ実行・履歴管理
 * ※ガチャロジック（確率計算）はサーバー側で実装
 */
interface GachaRepository {

    /** 現在有効なバナー一覧を取得 */
    suspend fun getActiveBanners(): List<GachaBanner>

    /** ガチャを実行（1回 or 10連） */
    suspend fun pullGacha(bannerId: String, count: Int = 1): List<GachaResult>

    /** ガチャ履歴を取得 */
    suspend fun getGachaHistory(bannerId: String? = null, limit: Int = 20): List<GachaResult>
}
