package org.example.project.features.study

import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.repository.StudyRepository

/**
 * 勉強タイマーのユースケース
 * セッション完了時の報酬リクエスト・オフライン対応
 */
class StudyUseCase(
    private val studyRepository: StudyRepository
) {
    /**
     * 勉強セッション完了処理
     * サーバーに送信し、報酬計算結果を受け取る
     */
    suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String? = null
    ): StudyCompleteResult {
        return studyRepository.completeSession(
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            userCharacterId = userCharacterId
        )
    }

    /**
     * オフライン時にセッションをローカルに保存
     * オンライン復帰時に自動同期される
     */
    suspend fun saveOfflineSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean
    ) {
        val session = org.example.project.domain.model.StudySession(
            id = "", // ローカル生成のUUIDに置き換え予定
            userId = "",
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            createdAt = endedAt
        )
        studyRepository.savePendingSession(session)
    }
}
