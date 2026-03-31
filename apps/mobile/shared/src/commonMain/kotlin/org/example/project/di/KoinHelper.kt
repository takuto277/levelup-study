package org.example.project.di

import org.example.project.features.home.HomeViewModel
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
    startKoin {
        modules(sharedModule)
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
