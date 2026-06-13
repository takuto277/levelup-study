package org.example.project.features.study

import kotlin.random.Random
import kotlinx.datetime.Clock
import org.example.project.domain.model.PendingStudyCompletion
import org.example.project.domain.model.StudyCompleteResult
import org.example.project.domain.repository.StudyRepository

class StudyCompletionException(
    message: String,
    val clientSessionId: String?,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 勉強タイマーのユースケース
 * セッション完了時の報酬リクエスト・オフライン対応
 */
class StudyUseCase(
    private val studyRepository: StudyRepository
) {
    /**
     * 勉強セッション完了処理。クライアント生成IDで冪等性を担保。
     */
    suspend fun completeSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String? = null,
        defeatNormalCount: Int = 0,
        defeatBossCount: Int = 0,
        difficultyMultiplier: Double = 1.0,
        clientSessionId: String = "${Clock.System.now().toEpochMilliseconds()}_d${Random.nextInt(1_000_000)}"
    ): StudyCompleteResult {
        val id = clientSessionId
        return try {
            studyRepository.completeSession(
                category = category,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = durationSeconds,
                isCompleted = isCompleted,
                userCharacterId = userCharacterId,
                defeatNormalCount = defeatNormalCount,
                defeatBossCount = defeatBossCount,
                difficultyMultiplier = difficultyMultiplier,
                clientSessionId = id
            ).copy(clientSessionId = id)
        } catch (e: Exception) {
            throw StudyCompletionException(
                message = e.message ?: "study completion failed",
                clientSessionId = id,
                cause = e
            )
        }
    }

    /**
     * 未同期の勉強完了をローカルキューへ保存（オンライン復帰後に [StudyRepository.syncPendingSessions] で送信）
     */
    suspend fun savePendingStudyCompletion(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        userCharacterId: String? = null,
        defeatNormalCount: Int = 0,
        defeatBossCount: Int = 0,
        difficultyMultiplier: Double = 1.0,
        isTrainingGround: Boolean = false,
        clientSessionId: String? = null
    ) {
        val localId = clientSessionId
            ?: "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1_000_000)}"
        studyRepository.savePendingCompletion(
            PendingStudyCompletion(
                localId = localId,
                category = category,
                startedAt = startedAt,
                endedAt = endedAt,
                durationSeconds = durationSeconds,
                isCompleted = isCompleted,
                userCharacterId = userCharacterId,
                defeatNormalCount = defeatNormalCount,
                defeatBossCount = defeatBossCount,
                difficultyMultiplier = difficultyMultiplier,
                isTrainingGround = isTrainingGround
            )
        )
    }

    /**
     * シンプルタイマー用のオフライン保存（討伐なし）
     */
    suspend fun saveOfflineSession(
        category: String?,
        startedAt: String,
        endedAt: String,
        durationSeconds: Int,
        isCompleted: Boolean,
        clientSessionId: String? = null
    ) {
        savePendingStudyCompletion(
            category = category,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            isCompleted = isCompleted,
            clientSessionId = clientSessionId
        )
    }
}
