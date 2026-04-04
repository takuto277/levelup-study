# タスク: 全画面データ連動・Go↔KMP API統合

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-02 |
| ステータス | 完了 |
| 担当 | AI |

## 概要
Go バックエンドのAPIレスポンス形式と KMP の DTO が不一致であるため、全画面でバックエンド接続が失敗しモックデータにフォールバックしている。
レスポンス形式を統一し、全画面をバックエンドと接続。ガチャ・勉強後のデータ同期、パーティ先頭キャラのホーム/勉強画面連動を実装する。

## 要件
- [ ] Go バックエンドの全API レスポンスを KMP DTO と一致させる
- [ ] ガチャ実行後に石の数が即反映される
- [ ] ガチャで取得したキャラが編成画面の一覧に入る
- [ ] 勉強完了後に累計時間・石・ゴールドがホームに反映される
- [ ] 編成画面がバックエンドのデータを表示する（モック廃止）
- [ ] 冒険画面がバックエンドのダンジョンデータを表示する
- [ ] 記録画面が実際の勉強履歴を表示する
- [ ] ホームのキャラ＝パーティの先頭キャラ
- [ ] 勉強中のキャラ＝パーティの先頭キャラ

## 影響範囲

### Go バックエンド
- `backend/internal/handler/game_handler.go` — リストレスポンスをラッパーオブジェクトに変更
- `backend/internal/handler/master_handler.go` — 同上
- `backend/internal/service/gacha_service.go` — レスポンスに updated_user を追加

### KMP 共有モジュール
- `data/remote/dto/GachaDto.kt` — GachaPullResponse を Go レスポンスに合わせる
- `data/repository/GachaRepositoryImpl.kt` — ガチャ後にユーザーキャッシュ更新
- `features/party/PartyViewModel.kt` — モック廃止、バックエンド接続
- `features/quest/QuestViewModel.kt` — モック廃止、バックエンド接続
- `features/analytics/AnalyticsViewModel.kt` — セッション履歴接続
- `features/home/HomeUseCase.kt` — パーティ先頭キャラ連動確認
- `features/study/StudyQuestViewModel.kt` — パーティ先頭キャラ表示

## 実装手順
1. Go: 全APIリストレスポンスをラッパーオブジェクト形式に統一
2. Go: ガチャPull レスポンスに updated_user を追加
3. KMP: GachaPullResponse DTO を Go 形式に合わせる
4. KMP: GachaRepositoryImpl にガチャ後のユーザーデータ更新を追加
5. KMP: PartyViewModel をバックエンド接続に切り替え
6. KMP: QuestViewModel をバックエンド接続に切り替え
7. KMP: AnalyticsViewModel のセッション履歴を接続
8. KMP: ホーム・勉強画面のキャラをパーティ先頭に連動
9. ビルド確認・API動作確認

## 実装詳細

### 1. Go レスポンス形式の不一致
Go は `respondJSON(w, 200, list)` でフラット配列 `[{...}]` を返すが、
KMP は `{"characters": [{...}]}` のようなラッパーオブジェクトを期待している。
→ Go 側で `map[string]interface{}{"characters": list}` のようにラップして返す。

### 2. ガチャ Pull レスポンスのDTO不一致
Go: `{results, stones_spent, remaining_stones}` の `GachaPullResult{result_type, item_id, name, rarity, is_new, pity_count}`
KMP: `{results, updated_user}` の `GachaResultResponse{id, user_id, banner_id, result_type, result_item_id, pity_count, created_at}`
→ KMP側をGo形式に合わせ、Go側にupdated_userを追加する混合アプローチ。

## リスク・注意点
- Go レスポンス変更により既存のKMP Gateway が動く必要がある
- モックデータ削除により、バックエンド未起動時は画面が空になる

## テスト計画
- [ ] 各マスタAPIのレスポンス形式確認（curl）
- [ ] ガチャ実行→石の減少→キャラ取得→編成画面に反映
- [ ] 勉強完了→石・時間増加→ホームに反映→記録に反映
- [ ] iOS/Androidビルド成功

## 結果・振り返り
完了後に記入する。
