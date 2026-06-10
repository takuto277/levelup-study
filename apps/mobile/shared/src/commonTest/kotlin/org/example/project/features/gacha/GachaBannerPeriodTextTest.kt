package org.example.project.features.gacha

import kotlin.test.Test
import kotlin.test.assertEquals

class GachaBannerPeriodTextTest {

    @Test
    fun formatsIsoDateRangeAsJapaneseLabel() {
        val label = gachaBannerPeriodLabel(
            startAt = "2026-06-01",
            endAt = "2026-06-30",
        )

        assertEquals("開催 2026年6月1日 〜 2026年6月30日", label)
    }

    @Test
    fun usesDashForBlankDateAndRawTextForUnknownFormat() {
        val label = gachaBannerPeriodLabel(
            startAt = " ",
            endAt = "soon",
        )

        assertEquals("開催 — 〜 soon", label)
    }
}
