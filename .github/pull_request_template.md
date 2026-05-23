Fixes #<!-- AUTO_FIXES_ISSUE -->

## なぜやったのか（背景・目的）

<!-- AUTO_WHY -->

## 何が変わったのか（変更内容）

<!-- AUTO_WHAT -->

## どうなるのか（期待される効果・運用）

<!-- AUTO_EFFECT -->

## ユーザーがやるべきこと（マージ前）

<!-- レビュー・マージ判断はユーザー。Issue 連携と説明文は Auto open PR / AI が自動生成します。 -->

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

<!-- AUTO_ISSUE_REF -->

- 本文先頭の **`Fixes #番号`** があると、`main` マージ時に Issue が **自動クローズ** されます
- `docs/tasks/issue-body-{ブランチslug}.md` があるブランチは push 時に **Issue 自動起票 + PR 本文生成** されます
