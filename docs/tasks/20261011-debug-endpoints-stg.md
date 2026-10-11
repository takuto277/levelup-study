# stg でデバッグ用エンドポイント（石付与）を有効化

作成日: 2026-10-11
ブランチ: `feat/debug-endpoints-stg`

## 目的

stg 環境でも「石・ゴールド増減」デバッグAPI（`POST /api/v1/debug/users/{userID}/currencies`）を使えるようにする。検証時にアプリから通貨を付与できるようにするため。

## 背景

- デバッグルートは `router.go` で **`DevMode == true` のときだけ登録**されていた（`if sec.DevMode { ... }`）。
- `DevMode=true` は **JWT 認証と OwnerGuard を無効化**する（ローカル開発専用）ため、公開 stg で有効化するのは危険。
- その結果、stg（`DEV_MODE=false`）では該当エンドポイントが存在せず **404**。

## 方針

「デバッグエンドポイントの有効化」と「認証バイパス（DevMode）」を**分離**する。

- 新フラグ **`ENABLE_DEBUG_ENDPOINTS`**（`SecurityConfig.DebugEndpoints`）を追加。
- ルート登録条件: `sec.DevMode || sec.DebugEndpoints`。
- **DevMode が false のまま**（＝JWT + OwnerGuard 維持）デバッルートのみ有効化できる。
  - これにより stg では「**認証済みユーザーが自分の通貨のみ**増減」に限定される。
- `render.yaml` の **stg サービスにのみ** `ENABLE_DEBUG_ENDPOINTS=true` を設定（**prod には設定しない**）。

## 実装詳細

- `internal/router/router.go`: `SecurityConfig` に `DebugEndpoints bool` を追加。デバッルート登録を `sec.DevMode || sec.DebugEndpoints` に変更。
- `cmd/api/main.go`: `ENABLE_DEBUG_ENDPOINTS`（大小無視で "true"）を読み、`SecurityConfig` へ渡す。有効時はログ出力。
- `render.yaml`: stg（`levelup-study-api-stg`）の env に `ENABLE_DEBUG_ENDPOINTS=true`。

## セキュリティ

- stg では **認証（JWTAuth）と OwnerGuard が有効なまま**。他人の通貨は変更不可。
- **prod では無効**（`ENABLE_DEBUG_ENDPOINTS` 未設定）。本番で有効化しないこと。

## 完了条件

- [ ] stg で `POST /api/v1/debug/users/{userID}/currencies` が 200 を返す（自分のトークンで）
- [ ] prod では 404 のまま

## 影響範囲

- backend: `internal/router/router.go`, `cmd/api/main.go`
- `render.yaml`
