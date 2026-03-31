package org.example.project.features.home

/**
 * ホーム画面のユーザー操作（Intent）
 */
sealed interface HomeIntent {
    /** ホーム情報をリフレッシュ */
    data object Refresh : HomeIntent

    /** 勉強開始ボタンをタップ */
    data object StartStudy : HomeIntent

    /** メインキャラクターをタップ */
    data object TapMainCharacter : HomeIntent
}
