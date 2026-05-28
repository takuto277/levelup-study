---
targets: [cursor]
globs:
  - docs/tasks/issue-body-*.md
  - docs/tasks/pr-body-*.md
  - AGENTS.md
  - .cursor/skills/**/*
---
# PR 作成ルール（AI 担当）

## 品質ゲート

1. **push 前** に `./scripts/validate-pr.sh` を実行（[AGENTS.md](../../AGENTS.md)）
2. iosApp 変更時は macOS で `./scripts/validate-pr.sh --ios` も
3. GitHub Actions は **push 後のセーフティネット**（lint/test）

## Issue / PR（gh CLI + スキル）

1. **`docs/tasks/issue-body-{slug}.md`** — `./scripts/create-issue.sh` または github-issue-create-from-plan
2. **実装** → `./scripts/validate-pr.sh`
3. **`docs/tasks/pr-body-{slug}.md`** — github-pr-create テンプレ
4. **`git push`** → **`gh pr create --body-file`**

## ユーザーがやるべきこと

- **CI** — Checks タブ green
- **レビュー依頼**
- **（該当時）手元確認**

Issue 連携・説明文・validate 記載は **ユーザー TODO に載せない**。

## issue-body ファイル形式

```markdown
# Issue: 短いタイトル

## 背景
## 目的
## スコープ
## 受け入れ条件
```
