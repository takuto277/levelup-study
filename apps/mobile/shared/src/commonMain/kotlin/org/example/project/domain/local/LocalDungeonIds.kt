package org.example.project.domain.local

/**
 * サーバーのマスタに載せない、アプリ同梱のダンジョン ID。
 * [org.example.project.features.quest.QuestUseCase] が一覧に混ぜる。
 */
object LocalDungeonIds {
    const val TRAINING_GROUND: String = "__app_local_training_ground__"

    const val TRAINING_GROUND_NAME: String = "訓練場"

    fun isTrainingGround(id: String?): Boolean = id != null && id == TRAINING_GROUND
}
