# ホーム/戦闘中のプレイヤー表示を選択中キャラの画像に

作成日: 2026-10-11
ブランチ: `feat/home-battle-character-image`

## 目的

ホーム画面・戦闘中のプレイヤーが常に同梱の汎用スプライト（実質 00001 固定）で表示されていたのを、**選択中（パーティ先頭＝メイン）キャラクターの画像**を表示するようにする。

## 背景

- 戦闘は `StudyQuestUiState.partyLeadImageUrl` を既に保持しているが UI で未使用だった。ホームは `HomeUiState.mainCharacter` を保持。
- 固定値（同梱スプライト）で描画していたため、獲得したキャラの絵が反映されなかった。

## 方針

- プレイヤー描画の中央関数に **任意の `imageUrl`** を足し、非空ならその画像を表示、空なら従来の同梱スプライト／絵文字にフォールバック。
- 選択中キャラの画像URLは、
  - Android 戦闘: `uiState.partyLeadImageUrl` を描画ヘルパーへ引き回し
  - Android ホーム: `homeState.mainCharacter?.character?.imageUrl`
  - iOS 戦闘: `Environment(\.partyLeadImageUrl)` を `StudyQuestScreenView` から注入し、`PlayerSpriteView` / `AnimatedSpriteView` が参照
  - iOS ホーム: `homeState?.mainCharacter?.character?.imageUrl`

## 実装詳細

- Android `components/BattleSprites.kt`: `PlayerSprite(mode, size, modifier, url)` に拡張。`url` 非空なら Coli `AsyncImage`。
- Android `features/study/StudyQuestScreenView.kt`: `AdventureScene` / `TrainingGroundLayer` / `BattleConfrontationLayer` / `BreakScene` に `playerImageUrl` を追加し、`MainQuestView` から `uiState.partyLeadImageUrl` を渡す。
- Android `features/home/HomeTabContent.kt`: メインキャラの `imageUrl` を `PlayerSprite` に渡す。
- iOS `StudyQuestScreenView.swift`: `PartyLeadImageUrlKey`（Environment）を導入。`PlayerSpriteView` / `AnimatedSpriteView` は environment の URL があれば `AsyncImage`。`StudyQuestScreenView` から `.environment(\.partyLeadImageUrl, uiState.partyLeadImageUrl)`。
- iOS `HomeScreenView.swift`: メインキャラの `imageUrl` を `AsyncImage` で表示（無ければ従来の idle スプライト）。

## トレードオフ

- 画像はキャラごとに1枚（`image_url`）のため、**歩行/攻撃などのアニメーションは静止画**になる（フォールバック時のみ従来アニメ）。複数フレーム配信は別タスク。

## 完了条件

- [ ] ホームのプレイヤーが選択中キャラの画像になる
- [ ] 戦闘中のプレイヤーが選択中キャラの画像になる
- [ ] 画像が無い場合は従来のスプライト/絵文字にフォールバック

## 影響範囲

- apps/mobile: `components/BattleSprites.kt`, `features/study/StudyQuestScreenView.kt`, `features/home/HomeTabContent.kt`(Android), `StudyQuestScreenView.swift`, `HomeScreenView.swift`(iOS)
