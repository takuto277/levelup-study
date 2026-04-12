package org.example.project.features.record

data class RecordUiState(
    val totalStudyMinutes: Int = 0,
    val selectedPeriod: RecordPeriod = RecordPeriod.TODAY,
    val chartBars: List<ChartBar> = emptyList(),
    val periodTotalMinutes: Int = 0,
    val genreBreakdown: List<GenreStudyTime> = emptyList(),
    val selectedGenre: GenreInfo? = null,
    val streakDays: Int = 0,
    val todaySessions: Int = 0,
    val characterMessage: String = "",
    val characterEmoji: String = "🧙‍♂️",
    val isLoading: Boolean = false,
    val error: String? = null,
    // 月間セレクター用
    val selectedYear: Int = 2026,
    val selectedMonth: Int = 4,
    val availableMonths: List<YearMonth> = emptyList(),
    // 勉強時間サマリー（キャラ吹き出し用）
    val todayStudyMinutes: Int = 0,
    val weekStudyMinutes: Int = 0,
    val monthStudyMinutes: Int = 0
)

data class YearMonth(val year: Int, val month: Int) {
    val label: String get() = "${year}/${month}"
}

enum class RecordPeriod(val label: String) {
    TODAY("今日"),
    WEEKLY("週間"),
    MONTHLY("月間")
}

data class GenreInfo(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long
) {
    companion object {
        val MATH = GenreInfo("math", "数学", "🔢", 0xFF3B82F6)
        val SCIENCE = GenreInfo("science", "理科", "🔬", 0xFF10B981)
        val LANGUAGE = GenreInfo("language", "語学", "📝", 0xFFF59E0B)
        val PROGRAMMING = GenreInfo("programming", "プログラミング", "💻", 0xFF8B5CF6)
        val GENERAL = GenreInfo("general", "総合", "📚", 0xFFEF4444)
        val CREATIVE = GenreInfo("creative", "クリエイティブ", "🎨", 0xFFEC4899)
        /** マスタから消えた slug のセッションを記録上まとめて表示する */
        val DELETED_TOPIC = GenreInfo("__deleted_topic__", "削除済み課題", "📕", 0xFF64748B)
        val defaults: List<GenreInfo> = listOf(MATH, SCIENCE, LANGUAGE, PROGRAMMING, GENERAL, CREATIVE)
    }
}

data class ChartBar(
    /** 月間など従来の短い軸ラベル（週間では補助用） */
    val label: String,
    val minutes: Int,
    val genreMinutes: Map<GenreInfo, Int> = emptyMap(),
    /** yyyy-MM-dd。週間チャートで曜日色・日付行の算出に使う */
    val isoDate: String = ""
)

data class GenreStudyTime(
    val genre: GenreInfo,
    val minutes: Int,
    val ratio: Float
)
