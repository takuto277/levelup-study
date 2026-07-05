## Issues

- Close #164

## 背景

開発時に毎回 Seed 固定ユーザーを使うと、複数端末やクリーンインストール時の動作確認が困難だった。Issue #164 では Debug Session モード切替（Seed / Guest）を要求しており、Release ビルドでは匿名認証（Guest）によるセッションを必須とする方針となった。

## 概要

Debug Session モード切替（Seed / Guest）と、Guest モード時の Supabase Anonymous Sign-In を実装する。Seed 時は従来通り固定 JWT を使い、Guest 時は Supabase Auth で匿名サインインし、取得した JWT を端末の安全なストレージ（Android Keystore / iOS Keychain）に保存する。Release ビルドでは常に Guest モードとして動作する。

## 変更内容

- Backend: `POST /api/v1/auth/user` の Context キー修正と JWT `sub` からの UUID パース対応
- Backend: `UserRepository.Upsert` の追加
- Mobile: `SessionMode` / `SessionModeStore` を追加し、Seed/Guest の永続化を実装
- Mobile: `UserSessionStore` を更新し、`forceDevSeedUserId` の判定を `SessionMode` に統合
- Mobile: Android Keystore / iOS Keychain による `SecureSessionStore` expect/actual を実装
- Mobile: Supabase Anonymous Sign-In を行う `GuestAuthService` を追加
- Mobile: セッション初期化・切替を担当する `SessionManager` を追加
- Mobile: `AuthTokenProvider` を追加し、`ApiClient` のトークン解決を一本化
- Mobile: Settings UI（Android/iOS）に Seed/Guest 切替を追加
- Mobile: `MainActivity` / `iOSApp.swift` の起動時に `SessionManager` を初期化
- Mobile: Supabase URL/Anon Key を Gradle BuildConfig 経由で生成
- Mobile: `local.properties.example` に Supabase 設定を追記
- Mobile: `SessionModeTest` を追加

## 確認項目

- [x] Backend テスト / `go vet`
- [ ] Mobile KMP Android コンパイル（ローカル Java 未インストールのため未実行、CI で確認）
- [ ] Mobile KMP ユニットテスト（ローカル Java 未インストールのため未実行、CI で確認）
- [ ] Mobile KMP iOS フレームワークリンク（ローカル Java 未インストールのため未実行、CI で確認）

## 詳細

- `initializeSessionMode(isDebug)` は既存の Seed 初期化を維持しつつ、`SessionManager` に Guest 初期化を委譲する。
- `GuestAuthService` は `io.github.jan_supabase` を使用。コンパイル時に API 差異があれば CI フィードバックに基づいて修正する。
- iOS 向けには Swift `await` ではなく、`initializeSessionManagerAsync` ヘルパー経由で Kotlin コルーチンを起動する。
