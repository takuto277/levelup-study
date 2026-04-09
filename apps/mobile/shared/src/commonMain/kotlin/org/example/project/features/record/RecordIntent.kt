package org.example.project.features.record

sealed interface RecordIntent {
    data class SelectPeriod(val period: RecordPeriod) : RecordIntent
    data class SelectGenre(val genre: GenreInfo?) : RecordIntent
    data class SelectMonth(val year: Int, val month: Int) : RecordIntent
    data object Refresh : RecordIntent
}
