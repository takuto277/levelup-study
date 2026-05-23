Fixes #<!-- イシュー番号。例: Fixes #2 -->

## なぜやったのか（背景・目的）

<!-- この PR が必要になった理由・解決したい課題 -->

-

## 何が変わったのか（変更内容）

<!-- 主要なファイル・機能の差分。箇条書き推奨 -->

-

## どうなるのか（期待される効果・運用）

<!-- マージ後にユーザー/開発者/CI にどう効くか -->

-

## CI / 品質チェック

マージ前に **Checks タブ** で以下が green であること（変更パスに応じて自動実行）:

| チェック | 対象 | 内容 |
|----------|------|------|
| **iOS — CI** | `apps/mobile/iosApp/**` | SwiftLint + iOS Simulator ビルド |
| **Mobile — CI** | `apps/mobile/**` | KMP Android コンパイル |
| **Backend — CI** | `backend/**` | `go test` / `go vet` |
| **Asset pipeline validation** | `apps/mobile/assets/**` 等 | manifest / sync 検証 |
| **Master images validation** | `backend/assets/master/**` 等 | Supabase マスタ manifest |

- [ ] 上記 CI が green（該当 workflow のみで可）
- [ ] SwiftLint 違反なし（iOS 変更時）
- [ ] 手元で必要な動作確認済み

## 関連イシュー

<!-- 本文先頭の `Fixes #n` でマージ時にイシューが自動クローズされます -->
