Fixes #<!-- AUTO_FIXES_ISSUE -->

## なぜやったのか（背景・目的）

<!-- この PR が必要になった理由・解決したい課題 -->

-

## 何が変わったのか（変更内容）

<!-- 主要なファイル・機能の差分。箇条書き推奨 -->

-

## どうなるのか（期待される効果・運用）

<!-- マージ後にユーザー/開発者/CI にどう効くか -->

-

## ユーザーがやるべきこと（マージ前）

<!-- レビュー依頼前に確認。自動 PR でも Actions がチェックリストを生成します。 -->

<!-- AUTO_USER_CHECKLIST -->

## CI / 品質チェック（参考）

PR の **Checks タブ** で green を確認（変更パスに応じて自動実行）:

| チェック | 主な対象 |
|----------|----------|
| **iOS — CI** | `apps/mobile/iosApp/**` — SwiftLint + Simulator ビルド |
| **Mobile — CI** | `apps/mobile/**` — KMP Android コンパイル |
| **Backend — CI** | `backend/**` |
| **Asset pipeline validation** | `apps/mobile/assets/**` 等 |
| **Master images validation** | `backend/assets/master/**` 等 |

## 関連イシュー

- 本文先頭の **`Fixes #番号`** があると、`main` マージ時に Issue が **自動クローズ** されます
- `docs/tasks/issue-body-{ブランチslug}.md` があるブランチは push 時に Issue 番号の自動連携を試みます（手動追記は不要な場合あり）
