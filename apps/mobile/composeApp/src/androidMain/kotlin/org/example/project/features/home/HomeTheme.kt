package org.example.project.features.home

import androidx.compose.ui.graphics.Color

internal object HomeTheme {
    val BgColor = Color(0xFF0B1120)
    val BgDark2 = Color(0xFF0F172A)
    val CardWhite = Color(0xFF111B2E)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val AccentBlue = Color(0xFF3B82F6)
    val AccentIndigo = Color(0xFF6366F1)
    val AccentCyan = Color(0xFF22D3EE)
    val FireRed = Color(0xFFEF4444)
    val FireOrange = Color(0xFFF59E0B)
    val BarBg = Color(0xFF0F172A)
    val BarStroke = Color(0xFF263859)
    val NavCyan = Color(0xFF22D3EE)
    val NavDim = Color(0xFF64748B)
}

internal const val HOME_STUDY_MINUTES_KEY = "home_study_minutes"

internal fun snapStudyMinutesToValid(m: Int): Int {
    val c = m.coerceIn(1, 60)
    if (c <= 1) return 1
    if (c < 5) return 5
    return ((c + 2) / 5 * 5).coerceAtMost(60)
}

internal fun studyMinutesIncrease(current: Int): Int {
    val c = current.coerceIn(1, 60)
    if (c >= 60) return 60
    if (c <= 1) return 5
    val ceil5 = (c + 4) / 5 * 5
    return if (ceil5 > c) ceil5 else (c + 5).coerceAtMost(60)
}

internal fun studyMinutesDecrease(current: Int): Int {
    val c = current.coerceIn(1, 60)
    if (c <= 1) return 1
    if (c <= 5) return 1
    val floor5 = c / 5 * 5
    return if (floor5 < c) floor5 else (c - 5).coerceAtLeast(5)
}
