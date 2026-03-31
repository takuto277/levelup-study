package org.example.project.domain.repository

import org.example.project.domain.model.DungeonProgress
import org.example.project.domain.model.DungeonStage
import org.example.project.domain.model.MasterDungeon

/**
 * ダンジョンリポジトリ
 * マスタデータ・ステージ・進行状況の管理
 */
interface DungeonRepository {

    /** ダンジョンマスタ一覧を取得 */
    suspend fun getDungeons(): List<MasterDungeon>

    /** 特定ダンジョンのステージ一覧を取得 */
    suspend fun getDungeonStages(dungeonId: String): List<DungeonStage>

    /** ユーザーの全ダンジョン進行状況を取得 */
    suspend fun getAllProgress(): List<DungeonProgress>

    /** 特定ダンジョンの進行状況を取得 */
    suspend fun getProgress(dungeonId: String): DungeonProgress?
}
