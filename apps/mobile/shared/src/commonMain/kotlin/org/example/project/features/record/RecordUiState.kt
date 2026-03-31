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
    val selectedGenre: StudyGenre? = null,

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
 * 勉強ジャンル
 */
enum class StudyGenre(val label: String, val emoji: String, val colorHex: Long) {
    MATH("数学", "🔢", 0xFF3B82F6),
    SCIENCE("理科", "🔬", 0xFF10B981),
    LANGUAGE("語学", "📝", 0xFFF59E0B),
    PROGRAMMING("プログラミング", "💻", 0xFF8B5CF6),
    GENERAL("総合", "📚", 0xFFEF4444),
    CREATIVE("クリエイティブ", "🎨", 0xFFEC4899)
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
    val genreMinutes: Map<StudyGenre, Int> = emptyMap()
)

/**
 * ジャンル別の勉強時間
 */
data class GenreStudyTime(
    val genre: StudyGenre,
    /** そのジャンルの合計時間（分） */
    val minutes: Int,
    /** 全体に対する割合 (0.0 ~ 1.0) */
    val ratio: Float
)
