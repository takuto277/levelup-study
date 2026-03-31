package org.example.project.domain.repository

import org.example.project.domain.model.Party
import org.example.project.domain.model.PartySlot

/**
 * パーティ編成リポジトリ
 * パーティのCRUD管理
 */
interface PartyRepository {

    /** 現在のパーティ編成を取得 */
    suspend fun getParty(): Party

    /** スロットにキャラクターを配置 */
    suspend fun updateSlot(slotPosition: Int, userCharacterId: String): PartySlot

    /** スロットからキャラクターを外す */
    suspend fun removeFromSlot(slotPosition: Int)
}
