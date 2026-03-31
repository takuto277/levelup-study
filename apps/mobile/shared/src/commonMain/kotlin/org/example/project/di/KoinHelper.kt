package org.example.project.di

import org.koin.core.context.startKoin

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
