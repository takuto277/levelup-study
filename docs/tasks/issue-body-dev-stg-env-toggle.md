# Issue: デバッグ画面に dev/stg 環境切り替えトグルを追加

## 背景

現在、モバイルの API 接続先は `ApiRoutes.BASE_URL` にハードコードされた本番 URL（`https://levelup-study-api.onrender.com`）固定。ローカルの Docker 開発環境（`http://localhost:8080`）や stg（`https://levelup-study-api-stg.onrender.com`）を確認したい場合、コードを書き換えて再ビルドする必要がある。

特にローカル Docker を起動していないときに stg で動作確認したいケースがあり、デバッグビルド内でワンタップで切り替えられると開発効率が上がる。

## 目的

- デバッグビルドの設定画面に dev / stg の環境切り替えトグルを追加する
- デフォルトは dev（`http://localhost:8080`）
- stg（`https://levelup-study-api-stg.onrender.com`）への切り替えが即時に反映される
- prod への切り替えは本スコープ外（既存のまま）

## スコープ

### やること

- [x] `ApiRoutes` の `BASE_URL` を実行時に切り替え可能にする（`const val` → 動的プロパティ または `object` 内の `var`）
- [x] `ApiClient` が URL 変更を反映できるよう再生成 or 動的 baseUrl 対応
- [x] `Koin` DI で `ApiClient` が環境切替時に再作成される仕組み
- [x] `UserSessionStore` に `selectedEnvironment` の永続化（デバッグビルドのみ）
- [x] Android SettingsScreenDialog のデバッグセクション末尾に dev / stg 切替 UI
- [x] iOS SettingsScreenView の debugSection 末尾に同様の切替 UI
- [x] 環境切替時に `ApiClient` を再生成し、既存の ViewModel / Repository が新しい URL を使う

### やらないこと

- prod への切り替え（本番 URL は引き続きハードコード or リリースビルド固定）
- ビルドフレーバー / xcconfig による環境切替（将来の本格対応は別 Issue）
- Supabase プロジェクトの stg/prod 分離
- Android の `local.properties` / iOS の xcconfig 経由での URL 設定

## 受け入れ条件

- [ ] デバッグビルドの設定画面下部に「環境」セクションが表示され、dev / stg を選択できる
- [ ] デフォルトは dev（`http://localhost:8080`）が選択されている
- [ ] stg に切り替えると API 通信が `https://levelup-study-api-stg.onrender.com` に向く
- [ ] リリースビルドでは環境切替 UI が表示されず、本番 URL が使われる
- [ ] アプリ再起動後も選択した環境が維持される
- [ ] Android / iOS 両方で動作する

## 参照ファイル

- `apps/mobile/shared/.../core/network/ApiRoutes.kt` — BASE_URL 定義
- `apps/mobile/shared/.../core/network/ApiClient.kt` — HTTP クライアント
- `apps/mobile/shared/.../di/SharedModule.kt` — Koin DI
- `apps/mobile/shared/.../core/session/UserSessionStore.kt` — 永続化
- `apps/mobile/composeApp/.../settings/SettingsScreenDialog.kt` — Android 設定
- `apps/mobile/iosApp/iosApp/SettingsScreenView.swift` — iOS 設定
- `apps/mobile/shared/.../features/settings/SettingsViewModel.kt`
- `apps/mobile/shared/.../features/settings/SettingsUiState.kt`
- `docs/tasks/issue-body-dev-stg-prod-environments.md` — 3環境構成の全体計画
