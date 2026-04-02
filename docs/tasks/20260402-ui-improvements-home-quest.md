# タスク: ホーム・冒険画面のUI改善

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-02 |
| ステータス | 完了 |
| 担当 | AI |

## 概要
ユーザー体験向上のため、ホーム画面と冒険（クエスト）画面のUIを改善した。
具体的には、ダンジョン詳細表示時のレイアウト崩れの修正、ホーム画面への目的地表示の追加、およびジャンル選択の操作性向上を行った。

## 要件
- [x] **冒険画面**: ダンジョン詳細オーバーレイの「出発ボタン」が下タブと重ならないように修正した。
- [x] **ホーム画面**: 現在選択されている「向かうダンジョン名」をUIの適切な場所に表示するようにした。
- [x] **ホーム画面**: 勉強ジャンル選択をボタン式からピッカー（Picker）形式に変更した。

## 影響範囲
- `apps/mobile/iosApp/iosApp/QuestScreenView.swift`: ダンジョン詳細オーバーレイのレイアウト修正、KMP連携。
- `apps/mobile/iosApp/iosApp/HomeScreenView.swift`: ダンジョン名表示の追加、ジャンル選択のPicker化。
- `apps/mobile/shared/src/commonMain/kotlin/.../features/home/HomeUiState.kt`: 選択中のダンジョン名を保持するフィールドを追加。
- `apps/mobile/shared/src/commonMain/kotlin/.../features/home/HomeIntent.kt`: `SelectDungeon` インテントを追加。
- `apps/mobile/shared/src/commonMain/kotlin/.../features/home/HomeViewModel.kt`: ダンジョン名更新のロジックを追加。

## 実装手順
1. **KMP層の更新**: `HomeUiState` に `selectedDungeonName` を追加し、`HomeViewModel` で `SelectDungeon` インテントを処理するようにした。
2. **冒険画面の修正**: `DungeonDetailOverlay` の `ScrollView` 内に `Spacer(height: 120)` を追加し、フローティングタブバーとの干渉を解消。また、「出発する」タップ時に KMP の ViewModel へダンジョン名を通知するようにした。
3. **ホーム画面の修正**: ヘッダーの下に `destinationBanner` を追加して選択中のダンジョンを表示。ジャンル選択を `Picker` に置き換えた。

## 実装詳細

### 1. KMP層の状態追加 (`HomeUiState.kt`, `HomeViewModel.kt`)
- **変更内容**:
  ```kotlin
  // HomeUiState.kt
  data class HomeUiState(
      // ...
      val selectedDungeonName: String? = null
  )

  // HomeViewModel.kt
  is HomeIntent.SelectDungeon -> {
      _uiState.update { it.copy(selectedDungeonName = intent.name) }
  }
  ```
- **理由**: 画面を跨いで「選択されたダンジョン」を共有するため、KMP の `UiState` で一元管理するようにした。

### 2. 冒険画面のレイアウト修正 (`QuestScreenView.swift`)
- **変更内容**:
  ```swift
  // DungeonDetailOverlay 内
  ScrollView {
      VStack {
          // ...
          Spacer().frame(height: 120)
      }
  }
  ```
- **理由**: カスタムタブバーが浮いているため、スクロールの最下部でボタンが隠れてしまう問題を、十分な余白（120pt）を設けることで解決した。

### 3. ホーム画面のジャンル選択 Picker 化 (`HomeScreenView.swift`)
- **変更内容**:
  ```swift
  Picker("ジャンル", selection: $selectedGenre) {
      ForEach(genres, id: \.label) { genre in
          Text("\(genre.emoji) \(genre.label)").tag(genre.label)
      }
  }
  .pickerStyle(.menu)
  ```
- **理由**: 従来のボタン並列配置よりも省スペースで、iOS 標準の洗練された UI を提供するため。

## リスク・注意点
- **タブバーの高さ**: 固定値 120pt で対応したが、将来的にタブバーのデザインが変わる場合は再調整が必要。

## テスト計画
- [x] 冒険画面でダンジョンを選択し、詳細シートをスクロールした際に「出発ボタン」が正常にタップできる。
- [x] ホーム画面に、選択したダンジョンの名前がバナーとして表示される。
- [x] ジャンル選択が Picker で行え、選択状態が保持される。

## 結果・振り返り
- KMP 側に状態を持たせたことで、iOS 側での同期がスムーズに行えた。
- 実行計画書に詳細を記載するフローにより、実装の意図が明確になり、迷いなく作業を進めることができた。
