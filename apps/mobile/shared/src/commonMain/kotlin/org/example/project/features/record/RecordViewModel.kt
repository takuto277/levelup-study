package org.example.project.features.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.project.domain.model.MasterStudyGenre
import org.example.project.domain.model.StudySession
import org.example.project.domain.repository.StudyRepository

class RecordViewModel(
    private val recordUseCase: RecordUseCase,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var allSessions: List<StudySession> = emptyList()
    private var genreMap: Map<String, GenreInfo> = emptyMap()

    init { refreshData() }

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
            is RecordIntent.SelectMonth -> {
                _uiState.update { it.copy(selectedYear = intent.year, selectedMonth = intent.month) }
                recalculate()
            }
            is RecordIntent.Refresh -> refreshData()
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                try {
                    studyRepository.syncPendingSessions()
                } catch (_: Exception) { }
                val data = recordUseCase.loadRecordData()
                allSessions = data.sessions
                genreMap = buildGenreMap(data.genres)

                // 累計はセッションの合計秒数から計算（一貫性のため）
                val totalSeconds = allSessions.sumOf { it.durationSeconds.toLong() }
                val totalMinutes = (totalSeconds / 60).toInt()
                val streakDays = calculateStreakDays(allSessions)
                val todayCount = countTodaySessions(allSessions)
                val availableMonths = buildAvailableMonths(allSessions)

                val today = todayDateString()
                val todayMin = sessionMinutesForDates(allSessions, setOf(today))
                val weekMin = sessionMinutesForDates(allSessions, sundayStartWeekDateStrings(today).toSet())
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val monthDates = datesInMonth(now.year, now.monthNumber)
                val monthMin = sessionMinutesForDates(allSessions, monthDates)

                _uiState.update {
                    it.copy(
                        totalStudyMinutes = totalMinutes,
                        streakDays = streakDays,
                        todaySessions = todayCount,
                        mainCharacter = data.mainCharacter,
                        characterEmoji = "\uD83E\uDDD9\u200D\u2642\uFE0F",
                        characterMessage = "",
                        isLoading = false,
                        availableMonths = availableMonths,
                        selectedYear = now.year,
                        selectedMonth = now.monthNumber,
                        todayStudyMinutes = todayMin,
                        weekStudyMinutes = weekMin,
                        monthStudyMinutes = monthMin
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
        val genre = state.selectedGenre
        val today = todayDateString()

        val filteredSessions: List<StudySession> = when (state.selectedPeriod) {
            RecordPeriod.TODAY -> allSessions.filter { extractDate(it.startedAt) == today }
            RecordPeriod.WEEKLY -> {
                val dates = sundayStartWeekDateStrings(today).toSet()
                allSessions.filter { extractDate(it.startedAt) in dates }
            }
            RecordPeriod.MONTHLY -> {
                val dates = datesInMonth(state.selectedYear, state.selectedMonth)
                allSessions.filter { extractDate(it.startedAt) in dates }
            }
        }

        val dailyData = filteredSessions.groupBy { extractDate(it.startedAt) }

        val bars = when (state.selectedPeriod) {
            RecordPeriod.WEEKLY -> {
                val weekDays = sundayStartWeekDateStrings(today)
                weekDays.map { date ->
                    val daySessions = dailyData[date] ?: emptyList()
                    val genreMinutes = mutableMapOf<GenreInfo, Int>()
                    for (s in daySessions) {
                        val gi = resolveGenre(s.category)
                        genreMinutes[gi] = (genreMinutes[gi] ?: 0) + s.durationSeconds / 60
                    }
                    val filtered = if (genre != null) mapOf(genre to (genreMinutes[genre] ?: 0)) else genreMinutes.toMap()
                    val total = if (genre != null) (genreMinutes[genre] ?: 0) else genreMinutes.values.sum()
                    ChartBar(
                        label = formatDateLabel(date),
                        minutes = total,
                        genreMinutes = filtered,
                        isoDate = date
                    )
                }
            }
            else -> {
                val sortedDays = dailyData.keys.sorted()
                sortedDays.map { date ->
                    val daySessions = dailyData[date] ?: emptyList()
                    val genreMinutes = mutableMapOf<GenreInfo, Int>()
                    for (s in daySessions) {
                        val gi = resolveGenre(s.category)
                        genreMinutes[gi] = (genreMinutes[gi] ?: 0) + s.durationSeconds / 60
                    }
                    val filtered = if (genre != null) mapOf(genre to (genreMinutes[genre] ?: 0)) else genreMinutes.toMap()
                    val total = if (genre != null) (genreMinutes[genre] ?: 0) else genreMinutes.values.sum()
                    ChartBar(label = formatDateLabel(date), minutes = total, genreMinutes = filtered, isoDate = "")
                }
            }
        }

        val periodTotal = bars.sumOf { it.minutes }

        val genreTotals = mutableMapOf<GenreInfo, Int>()
        for (s in filteredSessions) {
            val gi = resolveGenre(s.category)
            genreTotals[gi] = (genreTotals[gi] ?: 0) + s.durationSeconds / 60
        }
        val grandTotal = genreTotals.values.sum().coerceAtLeast(1)
        val breakdown = genreTotals.entries
            .sortedByDescending { it.value }
            .map { GenreStudyTime(genre = it.key, minutes = it.value, ratio = it.value.toFloat() / grandTotal) }

        val encouragement = encouragementMessage(state.selectedPeriod, periodTotal)

        _uiState.update {
            it.copy(
                chartBars = bars,
                periodTotalMinutes = periodTotal,
                genreBreakdown = breakdown,
                characterMessage = encouragement
            )
        }
    }

    private fun encouragementMessage(period: RecordPeriod, periodMinutes: Int): String {
        if (periodMinutes < 1) return ""
        val lines = when (period) {
            RecordPeriod.TODAY -> listOf(
                "今日の1分、ちゃんと積み上がってるぞ",
                "その調子だ。続けていこう",
                "よくやった。身体にちゃんと入ってる",
                "集中できたな。次もいける",
                "小さくても前に進んでる"
            )
            RecordPeriod.WEEKLY -> listOf(
                "今週も頑張ったな",
                "7日分、見応えあるぞ",
                "週でここまでやるのは強い",
                "続けてるのが一番の武器だ",
                "今週の流れ、いい感じだ"
            )
            RecordPeriod.MONTHLY -> listOf(
                "今月も立派だ",
                "月で見ると、かなり太いぞ",
                "このペース、自信を持っていい",
                "一ヶ月続いた努力はでかい",
                "今月の積み重ね、えらいぞ"
            )
        }
        return lines[Random.nextInt(lines.size)]
    }

    private fun resolveGenre(category: String?): GenreInfo {
        if (category == null) return GenreInfo.GENERAL
        return genreMap[category] ?: GenreInfo.DELETED_TOPIC
    }

    private fun sessionMinutesForDates(sessions: List<StudySession>, dates: Set<String>): Int {
        return sessions.filter { extractDate(it.startedAt) in dates }.sumOf { it.durationSeconds / 60 }
    }

    private fun buildAvailableMonths(sessions: List<StudySession>): List<YearMonth> {
        val monthSet = mutableSetOf<YearMonth>()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        monthSet.add(YearMonth(now.year, now.monthNumber))
        for (s in sessions) {
            val date = extractDate(s.startedAt)
            val parts = date.split("-")
            if (parts.size >= 2) {
                val y = parts[0].toIntOrNull() ?: continue
                val m = parts[1].toIntOrNull() ?: continue
                monthSet.add(YearMonth(y, m))
            }
        }
        return monthSet.sortedWith(compareByDescending<YearMonth> { it.year }.thenByDescending { it.month })
    }

    private fun calculateStreakDays(sessions: List<StudySession>): Int {
        if (sessions.isEmpty()) return 0
        val dates = sessions.map { extractDate(it.startedAt) }.distinct().sorted().reversed()
        val today = todayDateString()
        if (dates.firstOrNull() != today) return 0
        var streak = 1
        for (i in 1 until dates.size) {
            if (isConsecutive(dates[i], dates[i - 1])) streak++ else break
        }
        return streak
    }

    private fun countTodaySessions(sessions: List<StudySession>): Int {
        val today = todayDateString()
        return sessions.count { extractDate(it.startedAt) == today }
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

        /** 今日を含む暦週: 日曜始まり・土曜終わり（7日）の ISO 日付リスト（日→土の順） */
        internal fun sundayStartWeekDateStrings(today: String): List<String> {
            val d = LocalDate.parse(today)
            val daysBackFromSunday = when (d.dayOfWeek) {
                DayOfWeek.SUNDAY -> 0
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
            }
            val sunday = d.minus(DatePeriod(days = daysBackFromSunday))
            return (0..6).map { dayOffset ->
                sunday.plus(DatePeriod(days = dayOffset)).toString()
            }
        }

        /** 日本語曜日1文字 */
        internal fun weekdayJpOneLetter(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "月"
            DayOfWeek.TUESDAY -> "火"
            DayOfWeek.WEDNESDAY -> "水"
            DayOfWeek.THURSDAY -> "木"
            DayOfWeek.FRIDAY -> "金"
            DayOfWeek.SATURDAY -> "土"
            DayOfWeek.SUNDAY -> "日"
        }

        /** M/d（先頭ゼロなしに近い表示） */
        internal fun formatMonthDayLabel(date: String): String {
            val parts = date.split("-")
            return if (parts.size >= 3) "${parts[1].trimStart('0')}/${parts[2].trimStart('0')}" else date
        }

        internal fun datesInMonth(year: Int, month: Int): Set<String> {
            val start = LocalDate(year, month, 1)
            val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
            val days = mutableSetOf<String>()
            var d = start
            while (d < nextMonth) {
                days.add(d.toString())
                d = d.plus(DatePeriod(days = 1))
            }
            return days
        }

        internal fun isConsecutive(earlier: String, later: String): Boolean {
            return try {
                val d1 = LocalDate.parse(earlier)
                val d2 = LocalDate.parse(later)
                d1.plus(DatePeriod(days = 1)) == d2
            } catch (_: Exception) { false }
        }
    }
}
