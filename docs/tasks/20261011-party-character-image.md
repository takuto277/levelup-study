# 編成/キャラ選択画面でキャラ画像を表示

作成日: 2026-10-11
ブランチ: `feat/party-character-image`

## 目的

編成画面・キャラクター選択画面で、各キャラクターのマスタ画像（`MasterCharacter.imageUrl`）を表示する。従来は全キャラが汎用の「冒険者スプライト」1枚で表示され、実質すべて同じ絵（00001相当）だった。

## 背景

- Android `PartyPlayerAvatar` / iOS `partyPlayerAvatar` は常に同梱の `sprite_player_*`（冒険者スプライト）を表示していた。
- `UserCharacter.character: MasterCharacter?` に `imageUrl` が含まれるため、それを表示すればよい。
- ガチャ結果の画像表示で `imageUrl` の経路は整備済み（PR #187）。

## 方針

- アバター関数に **`imageUrl`（任意）** を追加し、非空なら `AsyncImage` で表示、無ければ従来の同梱スプライト→絵文字へフォールバック。
- 呼び出し箇所（メインカード / パーティslot / 所持キャラ一覧 / 選択ピッカー / 詳細画面）すべてで各キャラの `imageUrl` を渡す。
- これにより「選択中のキャラクター」を切り替えたときも、そのキャラの絵に変わる。

## 実装詳細

- Android `PartyScreenView.kt`: `PartyPlayerAvatar(size, imageUrl, modifier)` を追加（Coil `AsyncImage`）。呼び出し5箇所に `master.imageUrl`（slotでは `master?.imageUrl`）を指定。
- iOS `PartyScreenView.swift`: `partyPlayerAvatar(size:url:)` を追加（SwiftUI `AsyncImage`）。同梱フォールバックを `bundledPlayerAvatar(size:)` に分離。呼び出し5箇所に `m.imageUrl` を指定。

## 完了条件

- [ ] 所持キャラ一覧で各キャラの画像が表示される（全て同じ絵にならない）
- [ ] キャラを選択/切替すると、そのキャラの画像に変わる
- [ ] 画像が無いキャラは従来のスプライト/絵文字にフォールバック

## 影響範囲

- apps/mobile: `features/party/PartyScreenView.kt`(Android), `PartyScreenView.swift`(iOS)

## 後続（未対応）

- 図鑑（Collection）画面の画像表示。
- 入手時にローカル保存（オフライン用）。
