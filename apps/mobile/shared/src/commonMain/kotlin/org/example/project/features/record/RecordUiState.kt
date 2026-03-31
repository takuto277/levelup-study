package org.example.project.features.record

/**
 * 記録画面の UI 状態
 */
data class RecordUiState(
    /** 累計勉強時間（分） */
    val totalStudyMinutes: Int = 0,

    /** 表示中の期間 */
    val selectedPeriod: RecordPeriod = RecordPeriod.TODAY,

    /** 棒グラフ用データ（期間によって粒度が変わる） */
    val chartBars: List<ChartBar> = emptyList(),

    /** 選択中期間の合計勉強時間（分） */
    val periodTotalMinutes: Int = 0,

    /** ジャンル別の勉強時間内訳 */
    val genreBreakdown: List<GenreStudyTime> = emptyList(),

    /** 選択中のジャンルフィルター（null = 全ジャンル表示） */
    val selectedGenre: GenreInfo? = null,

    /** 連続学習日数 */
    val streakDays: Int = 0,

    /** 今日のセッション数 */
    val todaySessions: Int = 0,

    /** キャラクター吹き出しメッセージ */
    val characterMessage: String = "",

    /** キャラクター絵文字 */
    val characterEmoji: String = "🧙‍♂️",

    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 表示期間
 */
enum class RecordPeriod(val label: String) {
    TODAY("今日"),
    WEEKLY("週間"),
    MONTHLY("月間")
}

/**
 * ジャンル表示情報（データ駆動）
 *
 * 旧 StudyGenre enum を置き換え。サーバーの m_study_genres / user_genres から
 * 動的に生成されるので enum ではなく data class にしている。
 * id は MasterStudyGenre.id (UUID) または UserGenre.id に対応。
 */
data class GenreInfo(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long
) {
    companion object {
        // デフォルトジャンル（サーバー未接続時のフォールバック / モックデータ用）
        val MATH = GenreInfo("math", "数学", "🔢", 0xFF3B82F6)
        val SCIENCE = GenreInfo("science", "理科", "🔬", 0xFF10B981)
        val LANGUAGE = GenreInfo("language", "語学", "📝", 0xFFF59E0B)
        val PROGRAMMING = GenreInfo("programming", "プログラミング", "💻", 0xFF8B5CF6)
        val GENERAL = GenreInfo("general", "総合", "📚", 0xFFEF4444)
        val CREATIVE = GenreInfo("creative", "クリエイティブ", "🎨", 0xFFEC4899)

        /** 全デフォルトジャンル */
        val defaults: List<GenreInfo> = listOf(MATH, SCIENCE, LANGUAGE, PROGRAMMING, GENERAL, CREATIVE)
    }
}

/**
 * 棒グラフ 1 本分のデータ
 */
data class ChartBar(
    /** ラベル（"月", "火", "4/1" 等） */
    val label: String,
    /** 勉強時間（分） */
    val minutes: Int,
    /** ジャンル別内訳（積み上げ用） */
    val genreMinutes: Map<GenreInfo, Int> = emptyMap()
)

/**
 * ジャンル別の勉強時間
 */
data class GenreStudyTime(
    val genre: GenreInfo,
    /** そのジャンルの合計時間（分） */
    val minutes: Int,
    /** 全体に対する割合 (0.0 ~ 1.0) */
    val ratio: Float
)
