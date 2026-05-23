# Issue: パーティ装備・ガチャ履歴 API・Analytics 整理（#13 / #20 / #18）

Fixes #13
Fixes #18
Fixes #20

## 背景

- #13: 武器装備 API はあるが `PartyViewModel.EquipWeapon` が未実装
- #20: `GachaRepository.ListByUser` はあるが HTTP API がない
- #18: `AnalyticsViewModel` が `RecordViewModel` 置き換え後も dead code

## 目的

コアゲームループの小さな穴を3件まとめて解消し、CI green の PR を出す。

## スコープ（本イシュー）

### #13 パーティ武器装備
- `PartyViewModel` から `equipWeapon` API 呼び出し
- Android / iOS 編成画面のキャラ詳細から武器選択 UI

### #20 ガチャ履歴 API
- `GET /api/v1/users/{userID}/gacha/history?limit=&offset=&banner_id=`
- Owner Guard 配下

### #18 Analytics 整理
- 未使用 `features/analytics/` 削除
- Koin 登録削除

## 受け入れ条件

- [ ] 編成画面から武器を変更でき API に反映される
- [ ] ガチャ履歴 API が認証済みユーザーで取得できる
- [ ] Analytics dead code が削除されている
- [ ] Backend / Mobile CI が green
