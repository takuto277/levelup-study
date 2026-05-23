package org.example.project.di

import org.example.project.core.session.UserSessionStore
import org.example.project.features.record.RecordViewModel
import org.example.project.features.settings.SettingsViewModel
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
 * 開発用: シードのテストユーザー ID をセッションに入れる（Go の DEV_MODE=true 向け）。
 *
 * @param useSeedUser `true` のときだけ [seedId] をセッションに使う。
 * @param forceSeedUserId `true` のときは毎回 [seedId] を上書き（KeyValueStore に古い UUID が残っていても seed と揃う）。
 *   `false` のときは **未ログイン時のみ** [seedId] をセットする（createUser 検証などでは `false` を推奨）。
 *
 * **Supabase について**: `make seed-remote` が入れるのは **public.users** の固定 UUID であり、
 * Supabase Auth（auth.users）のログイン UUID とは別。アプリのこの ID と DB の seed が一致していればよい。
 *
 * **DATABASE_URL**: `make seed-remote` と `make run` は同じ `.env` の接続先を見ること。片方だけ別 DB にすると 404／空データになる。
 */
fun setDevSession(useSeedUser: Boolean = true, forceSeedUserId: Boolean = false) {
    if (!useSeedUser) {
        UserSessionStore.setForceDevSeedUserId(false)
        return
    }
    val seedId = UserSessionStore.DEV_SEED_USER_ID
    UserSessionStore.setForceDevSeedUserId(forceSeedUserId)
    when {
        forceSeedUserId -> UserSessionStore.setSession(userId = seedId)
        !UserSessionStore.hasSession() -> UserSessionStore.setSession(userId = seedId)
    }
    println(
        "[LevelUpStudy] DevSession userId=${UserSessionStore.userId} (seed user1 = $seedId). " +
            "API の DATABASE_URL は seed / seed-remote を流した DB と同じであること。",
    )
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

fun getRecordViewModel(): RecordViewModel {
    return KoinPlatform.getKoin().get()
}

fun getSettingsViewModel(): SettingsViewModel {
    return KoinPlatform.getKoin().get()
}
