package org.example.project.domain.repository

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
        isCompleted: Boolean
    ): StudyCompleteResult

    /** セッション履歴を取得 */
    suspend fun getSessionHistory(limit: Int = 20, offset: Int = 0): List<StudySession>

    /** オフライン時の未同期セッションをローカルに保存 */
    suspend fun savePendingSession(session: StudySession)

    /** 未同期セッションをサーバーに一括送信 */
    suspend fun syncPendingSessions()
}
