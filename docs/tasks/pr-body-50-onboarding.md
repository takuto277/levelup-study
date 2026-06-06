## Issues

- Close #50

## 背景

初回ユーザーに対してコアループ（勉強→冒険→報酬→編成/ガチャ）を説明する導線がなかった。初めて起動したとき、何をすればいいかわからない状態だった。

## 概要

初回起動時（または設定からの再表示時）に4ステップのオンボーディング画面を表示。Android (Compose) / iOS (SwiftUI) 両方で実装。

## 変更内容

### KMP Shared
- **`KeyValueStore.kt`**: `isOnboardingDone()` / `setOnboardingDone()` / `resetOnboarding()` ヘルパーと `ONBOARDING_DONE_KEY` 定数を追加

### Android
- **`OnboardingScreen.kt`** (新規): 4ページ構成のオンボーディング画面。ページインジケーター、次へ/スキップ/開始ボタン付き
- **`App.kt`**: KeyValueStore のフラグをチェックし、未完了なら HomeScreenView の代わりに OnboardingScreen を表示
- **`HomeScreenView.kt`**: `onOpenOnboarding` コールバックを App から受け取り SettingsScreenDialog に伝播
- **`SettingsScreenDialog.kt`**: 「オンボーディングを再表示」ボタンを追加

### iOS
- **`OnboardingView.swift`** (新規): Android と同内容の4ページオンボーディング
- **`MainTabView.swift`**: `@AppStorage("onboarding_done")` で初回起動をチェック、未完了なら OnboardingView を表示
- **`SettingsScreenView.swift`**: 「オンボーディングを再表示」ボタンを追加

## オンボーディング構成

| ページ | 内容 |
|--------|------|
| 1 📚 | 勉強を始めよう — 勉強時間が戦闘力に |
| 2 ⚔️ | 冒険に出かけよう — ダンジョン解放と敵討伐 |
| 3 🏆 | 報酬を集めよう — 召喚と装備強化 |
| 4 ☁️ | いつでも同期 — オフライン保存と自動同期 |

## 確認項目

- [x] フォーマット
- [x] リント（Go vet pass）
- [ ] テスト（KMP コンパイルは CI で検証。Java 未インストール環境のためローカル未実行）
- [ ] 手動確認：初回起動でオンボーディング表示 → 完了後ホーム遷移
- [ ] 手動確認：2回目以降は自動表示されない
- [ ] 手動確認：設定 → オンボーディング再表示 → 完了後ホームに戻る
