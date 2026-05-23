---
name: github-implement-pr
description: >
  Issue 実装〜PR まで一連で進める入口。LevelUp では feature-delivery と併用。
  「実装して PR まで」「Issue #n 対応して」で使う。
---

# github-implement-pr（LevelUp Study）

[feature-delivery](../feature-delivery/SKILL.md) をベースに、友人リポ [u7chan/agent-skills](https://github.com/u7chan/agent-skills) の手順を LevelUp 向けに統合した入口スキル。

## 基本方針

- **validate は GitHub Actions 前にローカルで行う**（`./scripts/validate-pr.sh`）
- **PR 本文は [github-pr-create](../github-pr-create/SKILL.md) のテンプレ**（Actions テンプレに依存しない）
- `main` 直 commit 禁止。Issue がある場合は `docs/tasks/issue-body-{slug}.md` を用意
- 作業領域: 最初だけ worktree 要否を確認（不要なら feat ブランチのみ）
- 失敗時以外は止まらず進める

## 参照スキル

| 工程 | スキル |
|------|--------|
| ブランチ | [git-branch-create](../git-branch-create/SKILL.md) |
| コミット | [git-commit-message](../git-commit-message/SKILL.md) |
| validate | [AGENTS.md](../../../AGENTS.md) + `./scripts/validate-pr.sh` |
| テスト観点（UI 等） | [qa-test-design](../qa-test-design/SKILL.md) |
| PR 作成 | [github-pr-create](../github-pr-create/SKILL.md) |

## ワークフロー

1. **Issue / 依頼確認** — `gh issue view` または `docs/tasks/issue-body-*.md`
2. **ブランチ** — `feat/{slug}`（[git-branch-create](../git-branch-create/SKILL.md)）
3. **実行計画** — 3 ステップ以上なら `docs/tasks/` に計画書（[task-init](../../commands/task-init.md)）
4. **実装** — 最小差分、[project-context](../project-context/SKILL.md) 参照
5. **validate** — `./scripts/validate-pr.sh`（ios 触ったら `--ios`）
6. **（任意）QA 観点** — 画面/API 変更時 [qa-test-design](../qa-test-design/SKILL.md)
7. **コミット** — [git-commit-message](../git-commit-message/SKILL.md)
8. **push + PR** — [github-pr-create](../github-pr-create/SKILL.md)

## 停止条件

- 要求・完了条件が特定できない
- validate が直せない
- `gh` 未認証
- 破壊的操作が必要

## 禁止

- validate 未実行のまま「CI に任せる」
- シークレットの commit
- ユーザー明示なしの merge
