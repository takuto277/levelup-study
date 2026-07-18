## Issues

- Close #164

## 背景

開発時に毎回 Seed 固定ユーザーを使うと、複数端末やクリーンインストール時の動作確認が困難だった。Issue #164 では Debug Session モード切替（Seed / Guest）を要求しており、Release ビルドでは匿名認証（Guest）によるセッションを必須とする方針となった。

## 概要

Debug Session モード切替（Seed / Guest）と、Guest モード時の Supabase Anonymous Sign-In を実装する。Seed 時は従来通り固定 JWT を使い、Guest 時は Supabase Auth で匿名サインインし、取得した JWT を端末の安全なストレージ（Android Keystore / iOS Keychain）に保存する。Release ビルドでは常に Guest モードとして動作する。

本 PR では加えて、Guest モードを dev/stg 環境で切り替えられるよう Supabase 設定を環境別にし、セッション初期化失敗時の stale ユーザー ID による 401 混乱を防ぐガードも追加する。

## 変更内容

- Backend: `POST /api/v1/auth/user` の Context キー修正と JWT `sub` からの UUID パース対応
- Backend: `UserRepository.Upsert` の追加
- Mobile: `SessionMode` / `SessionModeStore` を追加し、Seed/Guest の永続化を実装
- Mobile: `UserSessionStore` を更新し、`forceDevSeedUserId` の判定を `SessionMode` に統合
- Mobile: Guest モードで `authToken` がない場合、KeyValueStore の stale `userId` を無視するガードを追加
- Mobile: Android Keystore / iOS Keychain による `SecureSessionStore` expect/actual を実装
- Mobile: Supabase Anonymous Sign-In を行う `GuestAuthService` を追加
- Mobile: `SupabaseConfigSelector` を追加し、dev/stg 環境ごとに Supabase URL / anon key を切り替え
- Mobile: `GuestAuthService` が環境切替後も新しい設定を拾えるよう SupabaseClient を都度作成
- Mobile: セッション初期化・切替を担当する `SessionManager` を追加
- Mobile: Seed / Guest 切替時にセッションとローカルユーザーキャッシュをクリア
- Mobile: `AuthTokenProvider` を追加し、`ApiClient` のトークン解決を一本化（診断ログを削除）
- Mobile: `HomeUseCase.ensureUser()` で Guest モードかつセッション未初期化時に新規ユーザーを作らずエラーにするガードを追加
- Mobile: `UserRepository.clearCache()` を追加し、モード切替時にローカルキャッシュを削除
- Mobile: Settings UI（Android/iOS）に Seed/Guest 切替を追加
- Mobile: `MainActivity` / `iOSApp.swift` の起動時に `SessionManager` を初期化し、環境復元時に Supabase 設定も切り替え
- Mobile: Supabase URL/Anon Key を Gradle BuildConfig 経由で生成（dev / stg 環境対応）
- Mobile: `local.properties.example` に stg 用 Supabase 設定を追記
- Mobile: `SessionModeTest` を追加

## 確認項目

- [x] Backend テスト / `go vet`
- [x] SwiftLint（iOS ソース、既存警告は残存）
- [ ] Mobile KMP Android コンパイル（ローカル Java 未インストールのため未実行、CI で確認）
- [ ] Mobile KMP ユニットテスト（ローカル Java 未インストールのため未実行、CI で確認）
- [ ] Mobile KMP iOS フレームワークリンク（ローカル Java 未インストールのため未実行、CI で確認）

## 詳細

- `initializeSessionMode(isDebug)` は既存の Seed 初期化を維持しつつ、`SessionManager` に Guest 初期化を委譲する。
- `GuestAuthService` は `io.github.jan.supabase` を使用。コンパイル時に API 差異があれば CI フィードバックに基づいて修正する。
- iOS 向けには Swift `await` ではなく、`initializeSessionManagerAsync` ヘルパー経由で Kotlin コルーチンを起動する。
- Guest モードで Supabase 設定が未設定または環境が不一致の場合、トーストでエラー内容を表示し stale ユーザーによる 401 を防ぐ。
