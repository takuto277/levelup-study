package org.example.project.di

import org.example.project.core.session.UserSessionStore
import org.example.project.features.analytics.AnalyticsViewModel
import org.example.project.features.record.RecordViewModel
import org.example.project.features.gacha.GachaViewModel
import org.example.project.features.home.HomeViewModel
import org.example.project.features.party.PartyViewModel
import org.example.project.features.quest.QuestViewModel
import org.example.project.features.study.StudyQuestViewModel
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

/**
 * iOS 側から Koin を初期化するためのヘルパー
 *
 * Swift 側での使い方:
 * ```swift
 * // iOSApp.swift の init() 内で
 * KoinHelperKt.doInitKoin()
 * ```
 */
fun initKoin() {
    if (runCatching { KoinPlatform.getKoin() }.isSuccess) return
    startKoin {
        modules(sharedModule)
    }
}

/**
 * 開発用: シードデータのテストユーザーでセッションを設定する
 * seed.sql の user1 UUID をセットし、API 接続テストを可能にする
 */
fun setDevSession() {
    if (!UserSessionStore.hasSession()) {
        UserSessionStore.setSession(userId = "00000000-0000-0000-0000-000000000001")
    }
}

/**
 * iOS 側から ViewModel を取得するためのヘルパー関数
 *
 * Kotlin/Native では reified generics が使えないため、
 * 各 ViewModel ごとに明示的な取得関数を用意する。
 */
fun getHomeViewModel(): HomeViewModel {
    return KoinPlatform.getKoin().get()
}

fun getStudyQuestViewModel(): StudyQuestViewModel {
    return KoinPlatform.getKoin().get()
}

fun getPartyViewModel(): PartyViewModel {
    return KoinPlatform.getKoin().get()
}

fun getQuestViewModel(): QuestViewModel {
    return KoinPlatform.getKoin().get()
}

fun getGachaViewModel(): GachaViewModel {
    return KoinPlatform.getKoin().get()
}

fun getAnalyticsViewModel(): AnalyticsViewModel {
    return KoinPlatform.getKoin().get()
}

fun getRecordViewModel(): RecordViewModel {
    return KoinPlatform.getKoin().get()
}
