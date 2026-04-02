# タスク: ホーム・冒険画面のUI改善

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-02 |
| ステータス | 進行中 |
| 担当 | AI |

## 概要
ユーザー体験向上のため、ホーム画面と冒険（クエスト）画面のUIを改善する。
具体的には、ダンジョン詳細表示時のレイアウト崩れの修正、ホーム画面への目的地表示の追加、およびジャンル選択の操作性向上を行う。

## 要件
- [ ] **冒険画面**: ダンジョン詳細オーバーレイの「出発ボタン」が下タブと重ならないように修正する。
- [ ] **ホーム画面**: 現在選択されている「向かうダンジョン名」をUIの適切な場所に表示する。
- [ ] **ホーム画面**: 勉強ジャンル選択をボタン式からピッカー（Picker）形式に変更する。

## 影響範囲
- `apps/mobile/iosApp/iosApp/QuestScreenView.swift`: ダンジョン詳細オーバーレイのレイアウト修正。
- `apps/mobile/iosApp/iosApp/HomeScreenView.swift`: ダンジョン名表示の追加、ジャンル選択のPicker化。
- `apps/mobile/shared/src/commonMain/kotlin/.../features/home/HomeUiState.kt`: 選択中のダンジョン名を保持するフィールドを追加。
- `apps/mobile/shared/src/commonMain/kotlin/.../features/home/HomeViewModel.kt`: ダンジョン名更新のロジックを追加。

## 実装手順
1. **KMP層の更新**: `HomeUiState` に `selectedDungeonName` を追加し、`HomeViewModel` で更新可能にする。
2. **冒険画面の修正**: `DungeonDetailOverlay` のレイアウトを調整し、フローティングタブバーとの干渉を防ぐ。
3. **ホーム画面の修正**: 目的地表示の追加と、ジャンル選択の `Picker` 化を行う。

## 実装詳細

### 1. KMP層の状態追加 (`HomeUiState.kt`, `HomeViewModel.kt`)
- **変更内容**:
  ```kotlin
  // HomeUiState.kt
  data class HomeUiState(
      // ... 既存フィールド
      val selectedDungeonName: String? = null // 追加
  )

  // HomeViewModel.kt
  fun onIntent(intent: HomeIntent) {
      when (intent) {
          // ...
          is HomeIntent.SelectDungeon -> {
              _uiState.update { it.copy(selectedDungeonName = intent.name) }
          }
      }
  }
  ```
- **理由**: ホーム画面で「これから向かうダンジョン」を表示するためには、UIの状態としてダンジョン名を保持する必要があるため。また、KMP側で管理することで、将来的に Android 側にも同様の変更を容易に適用できる。

### 2. 冒険画面のレイアウト修正 (`QuestScreenView.swift`)
- **変更内容**:
  ```swift
  // DungeonDetailOverlay 内
  ScrollView {
      VStack {
          // ... コンテンツ
          Spacer().frame(height: 100) // 追加
      }
  }
  ```
- **理由**: 現在の `MainTabView` のカスタムタブバーは `ignoresSafeArea(.all, edges: .bottom)` を使用して浮いているため、通常の `ScrollView` の下端がタブバーの背面に潜り込んでしまう。ボタンが隠れないように明示的なパディングが必要。

### 3. ホーム画面のジャンル選択 Picker 化 (`HomeScreenView.swift`)
- **変更内容**:
  ```swift
  Picker("ジャンル", selection: $selectedGenre) {
      ForEach(genres, id: \.label) { genre in
          Text("\(genre.emoji) \(genre.label)").tag(genre.label)
      }
  }
  .pickerStyle(.menu) // または .wheel
  ```
- **理由**: 選択肢（ジャンル）が増えた際に、ボタンを並べる形式だと画面を占有しすぎるため。`Picker` を使うことで、省スペースかつ iOS 標準の操作感を提供できる。

## リスク・注意点
- **タブバーの高さ**: デバイスによってセーフエリアの高さが異なるため、固定値（100pt等）での調整が適切か確認が必要。

## テスト計画
- 冒険画面でダンジョンを選択し、詳細シートをスクロールした際に「出発ボタン」がタブバーに隠れず、正常にタップできること。
- ホーム画面に、選択したダンジョンの名前が正しく表示されること。
- ジャンル選択が Picker でスムーズに行え、選択結果が勉強開始時に正しく引き継がれること。
