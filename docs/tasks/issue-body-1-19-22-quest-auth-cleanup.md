# Issue: 旧ホスト doc 整理・冒険報酬 API 連動・認証ミドルウェアテスト

Fixes #1
Fixes #19
Fixes #22

## 背景

- #1: Railway / Fly.io 向けドキュメントは Render 統一後も Issue が残存
- #19: 冒険タブの報酬が `QuestUseCase` でハードコード（gold=100 等）
- #22: `APIKeyAuth` / `OwnerGuard` / `JWTAuth` にテストが無い

## 目的

3件を最小差分で解消し、CI green の PR を出す。

## スコープ（本イシュー）

### #1 旧ホスト doc 整理
- リポジトリ全体で Railway / Fly.io 参照を確認（ルールの「使わない」記述以外）
- 完了確認を PR 本文に記載

### #19 冒険報酬 API 連動
- マスタ API の `stages[].drop_table` をパースして次ステージ報酬を表示
- `getDungeonStages` のレスポンス型を修正（dungeon 詳細 JSON 対応）
- ハードコード報酬を削除

### #22 認証ミドルウェアテスト
- `APIKeyAuth` / `OwnerGuard` / `JWTAuth` の httptest

## 受け入れ条件

- [ ] 冒険詳細に次ステージの gold / exp / stones がサーバー値で表示される
- [ ] `go test ./...` で新規ミドルウェアテストが通る
- [ ] Backend / Mobile CI が green
