package org.example.project.domain.repository

import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.model.StudySession

/**
 * 勉強セッションリポジトリ
 * セッション完了・履歴取得・オフライン同期を管理
 */
interface StudyRepository {

    /** 勉強セッションを完了し、サーバーに報酬計算をリクエスト */
    suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String? = null,
        defeatNormalCount: Int = 0,
        defeatBossCount: Int = 0,
        difficultyMultiplier: Double = 1.0
    ): StudyCompleteResult

    /** セッション履歴を取得 */
    suspend fun getSessionHistory(limit: Int = 20, offset: Int = 0): List<StudySession>

    /** オフライン／送信失敗時に未同期の完了ペイロードをローカルへ保存 */
    suspend fun savePendingCompletion(pending: PendingStudyCompletion)

    /** 未同期セッションをサーバーへ順次送信（成功分のみローカルから削除） */
    suspend fun syncPendingSessions()
}
