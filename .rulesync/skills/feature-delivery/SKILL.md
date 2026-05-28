---
name: feature-delivery
description: LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR → レビュー）。ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---

# Feature Delivery（Issue → PR → レビュー）

**Issue / PR / レビューはすべて `gh` CLI + スキル。** GitHub Actions は lint/test のみ（push 後セーフティネット）。

LevelUp 固有設定: [LEVELUP.md](../../.cursor/skills/LEVELUP.md)

## フロー概要

```
Plan（必要なら grill-me / grill-with-docs）
  → github-issue-create-from-plan または create-issue.sh
  → github-implement-pr（実装〜PR）
  → github-pr-review / github-pr-feedback-address（レビュー対応）
```

入口の詳細手順は [github-implement-pr](../../.cursor/skills/github-implement-pr/SKILL.md)。

## 1. Issue 起票

**Actions は使わない。** 次のいずれか:

```bash
./scripts/create-issue.sh --title "feat: ..." --body-file docs/tasks/issue-body-{slug}.md
./scripts/feature-start.sh --title "feat: 機能名" --body-file docs/tasks/issue-body-{slug}.md --branch-slug feature-slug
```

## 2. 実装

- [project-context](../../.cursor/skills/project-context/SKILL.md) で制約確認
- 3 ステップ以上 → `docs/tasks/` に実行計画書
- 大きい Issue は [git-worktree-create](../../.cursor/skills/git-worktree-create/SKILL.md) を検討
- 画面/API 変更時は [qa-test-design](../../.cursor/skills/qa-test-design/SKILL.md)

## 3. validate（PR 前・必須）

```bash
./scripts/validate-pr.sh
./scripts/validate-pr.sh --ios   # iosApp 変更時（macOS）
```

詳細: [AGENTS.md](../../AGENTS.md)

## 4. コミット

[git-commit-message](../../.cursor/skills/git-commit-message/SKILL.md) に従う。

## 5. PR 作成

1. `git push -u origin HEAD`
2. [github-pr-create](../../.cursor/skills/github-pr-create/SKILL.md) で `docs/tasks/pr-body-{slug}.md` を生成
3. `gh pr create --body-file docs/tasks/pr-body-{slug}.md`

## 6. レビュー対応

- [github-pr-review](../../.cursor/skills/github-pr-review/SKILL.md)
- [github-pr-feedback-address](../../.cursor/skills/github-pr-feedback-address/SKILL.md)

## 禁止事項

- validate 未実行で PR 完了報告
- GitHub Actions で Issue / PR を自動作成する
- ユーザー明示なしの commit / push / merge
