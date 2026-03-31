# Kotlin Multiplatform (KMP) アーキテクチャルール

このルールは、AI駆動開発で迷わないために**実装時の判断基準**を明文化したものです。

---

## 1. 基本原則（MUST）
1. **共有ロジック最大化**
     - ビジネスロジック、`UiState`、`ViewModel`、時間計算、報酬計算、バリデーションは KMP に置く。
2. **UIはネイティブ実装**
     - iOS: SwiftUI、Android: Jetpack Compose。
     - 画面描画や遷移はネイティブ側。
3. **画面終了制御の方針**
     - `dismiss()` 等のUI操作はネイティブで実行する。
     - ただし、終了の可否判定は KMP 側状態（例: `isFinished`）に従う。
4. **データ層方針（将来実装）**
     - ローカルDB: SQLDelight もしくは KMP対応 Room。
5. **DI方針（将来実装）**
     - KMP 側 DI は Koin。

---

## 2. 命名規約（MUST）

### 2.1 画面・View
- iOS: `HomeScreenView.swift`, `QuestScreenView.swift`
- Android: `HomeScreen.kt`, `QuestScreen.kt`
- 画面内コンポーネント: `*View`

### 2.2 KMP ViewModel / State / Intent
- `HomeViewModel`
- `HomeUiState`
- `HomeIntent`

画面単位で **同名プレフィックス** を必ず揃える。

---

## 3. 推奨ファイル階層（MUST）

将来 UseCase / Repository を足しやすいよう、最初から階層を分ける。

```text
apps/mobile/
    iosApp/iosApp/
        features/
            home/
                HomeScreenView.swift
                HomeComponentsView.swift
            quest/
                QuestScreenView.swift
            party/
                PartyScreenView.swift
            gacha/
                GachaScreenView.swift
            analytics/
                AnalyticsScreenView.swift
            study/
                StudyQuestView.swift
        navigation/
            MainTabView.swift

    shared/src/commonMain/kotlin/org/example/project/
        features/
            home/
                HomeViewModel.kt
                HomeUiState.kt
                HomeIntent.kt
            quest/
                QuestViewModel.kt
                QuestUiState.kt
                QuestIntent.kt
            party/
                PartyViewModel.kt
            gacha/
                GachaViewModel.kt
            analytics/
                AnalyticsViewModel.kt
            study/
                StudyQuestViewModel.kt
```

---

## 4. ViewModel実装ルール（MUST）
1. `MutableStateFlow` を内部保持し、外部公開は `StateFlow`。
2. UIからの入力は `onIntent(intent: XxxIntent)` に集約。
3. 時間進行（タイマー）は ViewModel 管理。
4. `UiState` は不変データクラスに限定。
5. プラットフォーム依存 API を直接呼ばない。

---

## 5. AI実装チェックリスト（PR前）
- [ ] docs の該当機能を確認した
- [ ] iOS / Android / KMP で命名を揃えた
- [ ] ViewModel は StateFlow 公開にした
- [ ] 画面遷移はネイティブ、状態判定はKMPに分離した
- [ ] 将来 UseCase / Repository を足せる階層になっている

---

## 6. 補足（SHOULD）
- 1ファイルが肥大化する場合は `UiState` / `Intent` を分離
- 画面ロジックが増えたら `Reducer` パターンへ段階移行
- テストはまず ViewModel の状態遷移テストから書く
