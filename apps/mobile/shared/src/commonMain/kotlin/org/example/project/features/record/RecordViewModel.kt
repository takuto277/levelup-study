package org.example.project.features.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.StudySession

class RecordViewModel(
    private val recordUseCase: RecordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var allSessions: List<StudySession> = emptyList()
    private var genreMap: Map<String, GenreInfo> = emptyMap()

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

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val data = recordUseCase.loadRecordData()
                allSessions = data.sessions
                genreMap = buildGenreMap(data.genres)

                val totalMinutes = data.user.totalStudySeconds.toInt() / 60
                val streakDays = calculateStreakDays(allSessions)
                val todayCount = countTodaySessions(allSessions)

                _uiState.update {
                    it.copy(
                        totalStudyMinutes = totalMinutes,
                        streakDays = streakDays,
                        todaySessions = todayCount,
                        characterEmoji = "\uD83E\uDDD9\u200D\u2642\uFE0F",
                        characterMessage = getCharacterMessage(totalMinutes),
                        isLoading = false
                    )
                }
                recalculate()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun buildGenreMap(genres: List<MasterStudyGenre>): Map<String, GenreInfo> {
        val map = mutableMapOf<String, GenreInfo>()
        for (g in genres) {
            val colorLong = try {
                g.colorHex.removePrefix("#").toLong(16) or 0xFF000000L
            } catch (_: Exception) {
                0xFF6B7280L
            }
            map[g.slug] = GenreInfo(id = g.id, label = g.label, emoji = g.emoji, colorHex = colorLong)
        }
        return map
    }

    private fun recalculate() {
        val state = _uiState.value
        val period = state.selectedPeriod
        val genre = state.selectedGenre

        val today = todayDateString()

        val filteredSessions: List<StudySession> = when (period) {
            RecordPeriod.TODAY -> allSessions.filter { s -> extractDate(s.startedAt) == today }
            RecordPeriod.WEEKLY -> {
                val dates = last7Days(today)
                allSessions.filter { s -> extractDate(s.startedAt) in dates }
            }
            RecordPeriod.MONTHLY -> {
                val dates = last30Days(today)
                allSessions.filter { s -> extractDate(s.startedAt) in dates }
            }
        }

        val dailyData: Map<String, List<StudySession>> = filteredSessions
            .groupBy { s -> extractDate(s.startedAt) }

        val sortedDays: List<String> = dailyData.keys.sorted()

        val bars: List<ChartBar> = sortedDays.map { date ->
            val daySessions: List<StudySession> = dailyData[date] ?: emptyList()
            val genreMinutes = mutableMapOf<GenreInfo, Int>()
            for (s in daySessions) {
                val gi = resolveGenre(s.category)
                genreMinutes[gi] = (genreMinutes[gi] ?: 0) + s.durationSeconds / 60
            }
            val filtered: Map<GenreInfo, Int> = if (genre != null) {
                mapOf(genre to (genreMinutes[genre] ?: 0))
            } else {
                genreMinutes.toMap()
            }
            val total = if (genre != null) (genreMinutes[genre] ?: 0) else genreMinutes.values.sum()
            ChartBar(
                label = formatDateLabel(date),
                minutes = total,
                genreMinutes = filtered
            )
        }

        val periodTotal = bars.sumOf { bar -> bar.minutes }

        val genreTotals = mutableMapOf<GenreInfo, Int>()
        for (s in filteredSessions) {
            val gi = resolveGenre(s.category)
            genreTotals[gi] = (genreTotals[gi] ?: 0) + s.durationSeconds / 60
        }
        val grandTotal = genreTotals.values.sum().coerceAtLeast(1)
        val breakdown: List<GenreStudyTime> = genreTotals.entries
            .sortedByDescending { entry -> entry.value }
            .map { entry ->
                GenreStudyTime(genre = entry.key, minutes = entry.value, ratio = entry.value.toFloat() / grandTotal)
            }

        _uiState.update {
            it.copy(
                chartBars = bars,
                periodTotalMinutes = periodTotal,
                genreBreakdown = breakdown
            )
        }
    }

    private fun resolveGenre(category: String?): GenreInfo {
        if (category == null) return GenreInfo.GENERAL
        return genreMap[category] ?: GenreInfo.GENERAL
    }

    private fun calculateStreakDays(sessions: List<StudySession>): Int {
        if (sessions.isEmpty()) return 0
        val dates = sessions.map { s -> extractDate(s.startedAt) }.distinct().sorted().reversed()
        val today = todayDateString()
        if (dates.firstOrNull() != today) return 0
        var streak = 1
        for (i in 1 until dates.size) {
            if (isConsecutive(dates[i], dates[i - 1])) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    private fun countTodaySessions(sessions: List<StudySession>): Int {
        val today = todayDateString()
        return sessions.count { s -> extractDate(s.startedAt) == today }
    }

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

    companion object {
        internal fun extractDate(isoTimestamp: String): String = isoTimestamp.take(10)

        internal fun formatDateLabel(date: String): String {
            val parts = date.split("-")
            return if (parts.size >= 3) "${parts[1].trimStart('0')}/${parts[2].trimStart('0')}" else date
        }

        internal fun todayDateString(): String {
            val now = Clock.System.now()
            return now.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }

        internal fun last7Days(today: String): Set<String> = lastNDays(today, 7)
        internal fun last30Days(today: String): Set<String> = lastNDays(today, 30)

        private fun lastNDays(today: String, n: Int): Set<String> {
            val date = LocalDate.parse(today)
            return (0 until n).map { i ->
                date.minus(DatePeriod(days = i)).toString()
            }.toSet()
        }

        internal fun isConsecutive(earlier: String, later: String): Boolean {
            return try {
                val d1 = LocalDate.parse(earlier)
                val d2 = LocalDate.parse(later)
                d1.plus(DatePeriod(days = 1)) == d2
            } catch (_: Exception) {
                false
            }
        }
    }
}
