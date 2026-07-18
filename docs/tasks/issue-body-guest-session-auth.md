## 背景

現在のモバイルアプリは開発用 Seed ユーザーで動作しているが、Release で端末ごとのユーザーデータを安全に分離する認証セッションがない。また、`UserSessionStore` の token 保存先は通常の設定ストレージであり、認証 token の保存先として適切ではない。

ログイン UI はまだ追加せず、Supabase Anonymous Sign-In で端末ごとの Guest User を作成する。将来ログイン機能を追加するときは、Guest User にメール/OAuth Identity をリンクし、同じ User ID と既存データを引き継げる構造にする。

## 目的

- Supabase が発行する匿名 User ID と JWT を使い、クライアント生成 ID を信用しない認証にする
- Guest Session を端末の安全なストレージに保存し、再起動後も同じユーザーを復元する
- Debug では既存 Seed と Guest を切り替え、Release は Guest 固定にする
- 将来のログイン Identity Link でデータ移行を不要にする

## スコープ

- Supabase Anonymous Sign-In
- access token / refresh token の安全な保存・復元・更新
- Debug 設定画面の `Seed / Guest` 切り替え
- Release の Guest 固定
- API 環境ごとの Guest Session 分離
- JWT `sub` による public user の作成・取得
- `/api/v1/auth/user` の認証 Context 不整合修正
- 初期化、再試行、セッション再作成が必要な状態の UI

## スコープ外

- メール、Apple、Google などのログイン UI
- Guest User への Identity Link の実装（Issue #8 で扱う）
- 複数端末間の復旧・同期 UI
- 匿名ユーザーの定期削除処理

## 設計方針

- Debug の初期値は `Seed` とし、設定画面から `Seed / Guest` を切り替える
- Release は常に `Guest` とし、切り替え UI と Seed token を含めない
- User ID は Supabase Auth が生成し、Backend は署名済み JWT の `sub` を信用する
- Guest Session は dev/stg/prod ごとに分離する
- Guest から Seed へ切り替えても Guest Session を削除しない
- Android は Keystore、iOS は Keychain を利用して token pair を保存する
- access token は期限前後に refresh し、回転した refresh token と一括保存する
- refresh token が無効な既存ユーザーを、別の匿名ユーザーへ黙って置き換えない
- service role key と JWT secret はモバイルへ含めない

## 受け入れ条件

- [ ] Debug ビルドで `Seed / Guest` を切り替えられる
- [ ] Debug の初期値は Seed である
- [ ] Release ビルドは Guest 固定である
- [ ] Guest 初回起動時に Supabase が匿名 User ID とセッションを発行する
- [ ] 再起動後も同じ端末・環境で同じ Guest User ID を復元できる
- [ ] access token / refresh token が安全な platform storage に保存される
- [ ] access token の期限切れ前後に refresh される
- [ ] JWT `sub` と public `users.id` が一致する
- [ ] Seed / Guest と API 環境の切り替えでデータが混在しない
- [ ] 無効な既存セッションから別 Guest を自動作成しない
- [ ] 将来、同じ Auth User ID にログイン Identity をリンクできる構造である
- [ ] Mobile / Backend の関連テストと `./scripts/validate-pr.sh` が成功する

## 実行計画書

- [`docs/tasks/20260705-guest-session-auth.md`](https://github.com/takuto277/levelup-study/blob/main/docs/tasks/20260705-guest-session-auth.md)

## 関連 Issue

- #8 Supabase Auth ログインとユーザー ID 紐付け

## 公式資料

- [Supabase Anonymous Sign-Ins](https://supabase.com/docs/guides/auth/auth-anonymous)
- [Supabase Kotlin: Anonymous Sign-In](https://supabase.com/docs/reference/kotlin/auth-signinanonymously)
- [Supabase Sessions](https://supabase.com/docs/guides/auth/sessions)
