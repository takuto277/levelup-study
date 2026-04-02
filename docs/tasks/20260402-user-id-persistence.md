# タスク: ユーザーID ローカル永続化

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-02 |
| ステータス | 完了 |
| 担当 | AI |

## 概要
アプリ起動時に毎回新しいユーザーIDを生成していたため、再起動するとサーバーとの紐付けが切れていた。
初回起動時にIDをローカルに保存し、2回目以降はそのIDでサーバーからデータを取得するようにする。

## 要件
- [x] 初回起動時にサーバーから発行されたユーザーIDをローカルに永続化する
- [x] 2回目以降の起動では保存済みIDを使ってサーバーからユーザーデータを取得する
- [x] iOS (NSUserDefaults) / Android (SharedPreferences) の両方で動作する
- [x] KMP の `expect/actual` パターンで実装し、外部ライブラリに依存しない

## 影響範囲
- `shared/src/commonMain/.../core/session/UserSessionStore.kt` — メモリ保持 → 永続化に変更
- `shared/src/commonMain/.../core/storage/KeyValueStore.kt` — 新規 (expect)
- `shared/src/androidMain/.../core/storage/KeyValueStore.android.kt` — 新規 (actual: SharedPreferences)
- `shared/src/iosMain/.../core/storage/KeyValueStore.ios.kt` — 新規 (actual: NSUserDefaults)
- `shared/build.gradle.kts` — expect/actual class 警告抑制フラグ追加

## 実装手順
1. `KeyValueStore` の `expect` インターフェースを `commonMain` に定義
2. Android 向けに `SharedPreferences` を使った `actual` 実装
3. iOS 向けに `NSUserDefaults` を使った `actual` 実装
4. `UserSessionStore` を `KeyValueStore` に依存するよう書き換え
5. Android / iOS 両方のコンパイルを確認

## リスク・注意点
- **KMP ライブラリ互換性**: 当初 `multiplatform-settings` ライブラリ (v1.3.0, Kotlin 2.1.0) を使用したが、プロジェクトの Kotlin 2.3.0 と klib メタデータが非互換でビルドエラーとなった。サードパーティ KMP ライブラリは Kotlin バージョン互換を必ず確認すること。
- **Android の `initKeyValueStore(context)`**: Android 側は `SharedPreferences` に `Context` が必要なため、Application の `onCreate` で初期化する必要がある。

## テスト計画
- アプリ初回起動でユーザーが自動作成され、IDがローカルに保存される
- アプリを kill → 再起動した際、同じユーザーIDでサーバーからデータを取得できる
- ログアウト（`UserSessionStore.clear()`）後に再度ユーザーが作成される

## 結果・振り返り
- `expect/actual` パターンで実装し、外部ライブラリ不要で解決できた
- KMP でサードパーティライブラリ追加時の Kotlin バージョン互換性問題を `.rulesync/rules/kmp.md` に追記した
- 今後は同様の問題が起きた際のトラブルシューティング手順がルールとして残る
