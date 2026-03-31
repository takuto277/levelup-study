package org.example.project.features.record

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 記録（Record）画面の ViewModel
 *
 * - 勉強記録の集計・表示を担当
 * - 期間切替（今日 / 週間 / 月間）でチャートデータを再計算
 * - ジャンルフィルターで表示を絞り込み
 * - 現在はモックデータ。将来的に StudyRepository を DI で注入
 */
class RecordViewModel {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    /** モックの勉強ログ（日別 × ジャンル別） */
    private val mockDailyRecords: List<DailyRecord> = generateMockRecords()

    init {
        refreshData()
    }

    fun onIntent(intent: RecordIntent) {
        when (intent) {
            is RecordIntent.SelectPeriod -> {
                _uiState.update { it.copy(selectedPeriod = intent.period, selectedGenre = null) }
                recalculate()
            }
            is RecordIntent.SelectGenre -> {
                val newGenre = if (_uiState.value.selectedGenre == intent.genre) null else intent.genre
                _uiState.update { it.copy(selectedGenre = newGenre) }
                recalculate()
            }
            is RecordIntent.Refresh -> refreshData()
        }
    }

    // ── 集計 ──────────────────────────────────────────

    private fun refreshData() {
        val totalMinutes = mockDailyRecords.sumOf { it.totalMinutes() }
        _uiState.update {
            it.copy(
                totalStudyMinutes = totalMinutes,
                streakDays = 12,
                todaySessions = 3,
                characterEmoji = "🧙‍♂️",
                characterMessage = getCharacterMessage(totalMinutes)
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val period = state.selectedPeriod
        val genre = state.selectedGenre

        // 期間に応じたレコードを取得
        val filteredRecords = when (period) {
            RecordPeriod.TODAY -> mockDailyRecords.takeLast(1)
            RecordPeriod.WEEKLY -> mockDailyRecords.takeLast(7)
            RecordPeriod.MONTHLY -> mockDailyRecords.takeLast(30)
        }

        // チャートバー生成
        val bars = filteredRecords.map { record ->
            val genreMinutes = if (genre != null) {
                mapOf(genre to (record.genreMinutes[genre] ?: 0))
            } else {
                record.genreMinutes
            }
            val total = if (genre != null) (record.genreMinutes[genre] ?: 0) else record.totalMinutes()
            ChartBar(
                label = record.label,
                minutes = total,
                genreMinutes = genreMinutes
            )
        }

        // 期間合計
        val periodTotal = bars.sumOf { it.minutes }

        // ジャンル別集計
        val genreTotals = mutableMapOf<StudyGenre, Int>()
        for (record in filteredRecords) {
            for ((g, min) in record.genreMinutes) {
                genreTotals[g] = (genreTotals[g] ?: 0) + min
            }
        }
        val grandTotal = genreTotals.values.sum().coerceAtLeast(1)
        val breakdown = genreTotals.entries
            .sortedByDescending { it.value }
            .map { (g, min) ->
                GenreStudyTime(
                    genre = g,
                    minutes = min,
                    ratio = min.toFloat() / grandTotal
                )
            }

        _uiState.update {
            it.copy(
                chartBars = bars,
                periodTotalMinutes = periodTotal,
                genreBreakdown = breakdown
            )
        }
    }

    // ── キャラクターメッセージ ────────────────────────

    private fun getCharacterMessage(totalMinutes: Int): String {
        return when {
            totalMinutes >= 6000 -> "100時間突破！もはや伝説の勇者だな！"
            totalMinutes >= 3000 -> "50時間超えか…大した修行量だ。"
            totalMinutes >= 1200 -> "いい調子だ！その努力は裏切らないぞ。"
            totalMinutes >= 600  -> "10時間も頑張ったのか。見直したぞ。"
            totalMinutes >= 60   -> "毎日の積み重ねが力になる。続けろよ。"
            else                 -> "まずは冒険に出てみよう！最初の一歩が大事だ。"
        }
    }

    // ── モックデータ ────────────────────────────────

    /**
     * 日別 × ジャンル別の勉強時間記録（内部モデル）
     */
    data class DailyRecord(
        val label: String,
        val genreMinutes: Map<StudyGenre, Int>
    ) {
        fun totalMinutes(): Int = genreMinutes.values.sum()
    }

    companion object {
        private fun generateMockRecords(): List<DailyRecord> {
            // 30日分のモックデータ
            val dayLabels30 = listOf(
                "3/3","3/4","3/5","3/6","3/7","3/8","3/9",
                "3/10","3/11","3/12","3/13","3/14","3/15","3/16",
                "3/17","3/18","3/19","3/20","3/21","3/22","3/23",
                "3/24","3/25","3/26","3/27","3/28","3/29","3/30",
                "3/31","4/1"
            )

            // 各日のジャンル別データ（擬似ランダム風に決め打ち）
            val patterns = listOf(
                mapOf(StudyGenre.MATH to 25, StudyGenre.LANGUAGE to 30),
                mapOf(StudyGenre.PROGRAMMING to 45, StudyGenre.GENERAL to 15),
                mapOf(StudyGenre.SCIENCE to 30, StudyGenre.MATH to 20, StudyGenre.CREATIVE to 10),
                mapOf(StudyGenre.LANGUAGE to 40, StudyGenre.PROGRAMMING to 20),
                mapOf(StudyGenre.MATH to 35, StudyGenre.SCIENCE to 25, StudyGenre.GENERAL to 15),
                mapOf(StudyGenre.PROGRAMMING to 50, StudyGenre.CREATIVE to 20),
                mapOf(StudyGenre.GENERAL to 20, StudyGenre.LANGUAGE to 25),
                mapOf(StudyGenre.MATH to 30, StudyGenre.SCIENCE to 35, StudyGenre.PROGRAMMING to 25),
                mapOf(StudyGenre.LANGUAGE to 45, StudyGenre.CREATIVE to 15),
                mapOf(StudyGenre.MATH to 40, StudyGenre.GENERAL to 25, StudyGenre.SCIENCE to 20),
                mapOf(StudyGenre.PROGRAMMING to 55, StudyGenre.MATH to 15),
                mapOf(StudyGenre.SCIENCE to 30, StudyGenre.LANGUAGE to 35),
                mapOf(StudyGenre.GENERAL to 25, StudyGenre.CREATIVE to 30, StudyGenre.MATH to 20),
                mapOf(StudyGenre.PROGRAMMING to 40, StudyGenre.SCIENCE to 20),
                mapOf(StudyGenre.MATH to 50, StudyGenre.LANGUAGE to 30, StudyGenre.GENERAL to 10),
            )

            return dayLabels30.mapIndexed { idx, label ->
                DailyRecord(
                    label = label,
                    genreMinutes = patterns[idx % patterns.size]
                )
            }
        }
    }
}
