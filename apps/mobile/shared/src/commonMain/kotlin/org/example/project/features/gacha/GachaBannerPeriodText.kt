package org.example.project.features.gacha

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * マスタの `start_at` / `end_at`（ISO 日付または日時）を一覧用の日本語1行にまとめる。
 */
fun gachaBannerPeriodLabel(startAt: String, endAt: String): String {
    fun formatOne(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return "—"
        return try {
            val datePart = s.take(10)
            val ld = LocalDate.parse(datePart)
            "${ld.year}年${ld.monthNumber}月${ld.dayOfMonth}日"
        } catch (_: Throwable) {
            try {
                val instant = Instant.parse(s)
                val l = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${l.year}年${l.monthNumber}月${l.dayOfMonth}日 " +
                    "${l.hour.toString().padStart(2, '0')}:${l.minute.toString().padStart(2, '0')}"
            } catch (_: Throwable) {
                s
            }
        }
    }
    return "開催 ${formatOne(startAt)} 〜 ${formatOne(endAt)}"
}
