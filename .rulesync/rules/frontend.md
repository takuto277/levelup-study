# Frontend Rules (Native UI)

SwiftUI (iOS) および Jetpack Compose (Android) に関するネイティブ UI 実装のルール。

## 原則
- UI は各プラットフォームのネイティブ技術で実装する。
- KMP の `ViewModel` から提供される `StateFlow` を購読して表示する。
- 画面遷移（Navigation）はネイティブ側で制御する。

## 命名規則
- iOS: `*ScreenView.swift`
- Android: `*Screen.kt`
