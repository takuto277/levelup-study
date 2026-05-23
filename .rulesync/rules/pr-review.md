---
targets: [cursor]
globs:
  - .github/pull_request_template.md
  - .github/workflows/open-pr*.yml
---
# PR レビュー前チェックリスト

PR 本文には次の **4 セクション** を必ず含める（テンプレート準拠）。

1. **なぜやったのか** — 背景・目的
2. **何が変わったのか** — 主要な変更
3. **どうなるのか** — マージ後の効果・運用
4. **ユーザーがやるべきこと** — レビュー依頼前のチェックリスト

## ユーザーがやるべきこと（標準項目）

Auto open PR は次をチェックリストとして自動挿入する。手動 PR も同様に埋める。

- **Issue 連携** — `Fixes #n`（自動設定済みなら確認のみ）
- **説明文** — 上記 3 セクションを記入
- **CI** — Checks タブですべて green
- **レビュー依頼** — 内容確認・レビュアー指定
- **（該当時）手元確認** — シミュレータ / API / スクリプト
- **（該当時）Issue 本文** — `docs/tasks/issue-body-{slug}.md` と実装の一致

追加のユーザー作業（デプロイ、Secrets 設定、手動 migrate 等）がある PR は、**同じ「ユーザーがやるべきこと」欄に箇条書きで追記**する。

## Fixes #n について

- `docs/tasks/issue-body-{ブランチslug}.md` があるブランチ → **Actions が `Fixes #n` を自動挿入**（手動不要なことが多い）
- 無い場合のみ PR 先頭に `Fixes #n` を手動記入
- `main` マージ時に Issue が自動クローズされるのは `Fixes` / `Closes` / `Resolves` 付き PR のみ

## 自動 PR の設定

- `open-pr-on-push.yml`: `draft: false`（ドラフト PR は作らない）
- テンプレートの `<!-- AUTO_USER_CHECKLIST -->` / `Fixes #<!-- AUTO_FIXES_ISSUE -->` を置換
