package org.example.project.features.home

import org.example.project.domain.local.LocalDungeonIds
import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.UserCharacter
import org.example.project.features.quest.QuestUseCase

data class HomeUiState(
    val totalStudySeconds: Long = 0,
    val stones: Int = 0,
    val gold: Int = 0,
    val mainCharacter: UserCharacter? = null,
    val displayName: String = "",
    val selectedDungeonId: String? = null,
    val selectedDungeonName: String? = null,
    /** 選択中ダンジョンの背景（マスタ image_url） */
    val selectedDungeonImageUrl: String? = null,
    val genres: List<MasterStudyGenre> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** ネット未接続時はダンジョンの代わりに訓練場モードで勉強する */
    val isOfflineTraining: Boolean = false
) {
    val formattedStudyTime: String
        get() {
            val hours = totalStudySeconds / 3600
            val minutes = (totalStudySeconds % 3600) / 60
            return "${hours}h ${minutes}m"
        }

    /**
     * 勉強クエストで樽打ち訓練 UI を使うか。
     * オフライン強制、またはアプリ同梱の「訓練場」ダンジョン選択時。
     * オンライン時も [isTrainingStudySession] なら報酬は [StudyQuestViewModel] から通常どおりサーバー同期される。
     */
    val isTrainingStudySession: Boolean
        get() = isOfflineTraining || LocalDungeonIds.isTrainingGround(selectedDungeonId)

    /**
     * ホームの「ダンジョン」チップ用の表示名。
     * サーバー名が無いときは前回キャッシュや同梱マスタで補完し、舞台コンテキストを失わない。
     */
    val adventureDungeonDisplayName: String
        get() {
            val explicit = selectedDungeonName?.trim()?.takeIf { it.isNotEmpty() }
            if (explicit != null) return explicit
            val id = selectedDungeonId ?: return "—"
            return QuestUseCase.bundledDisplayNameForDungeonId(id) ?: "—"
        }

    /**
     * オフライン時に、名前とは別に「いまの勉強が訓練扱い」などを一言で示す。
     */
    val adventureDungeonChipHint: String?
        get() {
            if (!isOfflineTraining) return null
            return if (!LocalDungeonIds.isTrainingGround(selectedDungeonId)) {
                "いまは訓練で進行"
            } else {
                "オフライン"
            }
        }
}
