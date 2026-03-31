package org.example.project.features.gacha

/**
 * 召喚（Gacha）画面のユーザー操作
 */
sealed interface GachaIntent {
    /** バナー一覧をリフレッシュ */
    data object Refresh : GachaIntent

    /** バナーを選択 → CONFIRM フェーズへ */
    data class SelectBanner(val bannerId: String) : GachaIntent

    /** バナー選択に戻る */
    data object BackToBannerSelect : GachaIntent

    /** 単発ガチャ（1回） */
    data class PullSingle(val bannerId: String) : GachaIntent

    /** 10連ガチャ */
    data class PullMulti(val bannerId: String) : GachaIntent

    /** 結果画面から「もう一度引く」→ CONFIRM へ */
    data object PullAgain : GachaIntent

    /** 結果画面から「やめる」→ BANNER_SELECT へ */
    data object DismissResults : GachaIntent

    /** エラーを閉じる */
    data object DismissError : GachaIntent
}
