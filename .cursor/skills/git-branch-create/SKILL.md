---
name: git-branch-create
description: LevelUp Study 向け feat ブランチ命名と作成
---

# git-branch-create

## 本リポジトリの慣習

```
feat/{issue-slug}
feat/{n}-{m}-{slug}-batch
fix/{slug}
chore/{slug}
```

例:

- `feat/agent-skills-workflow`
- `feat/9-12-14-17-24-batch`

Issue 番号がある場合は slug または batch 名に含める。

## 手順

1. `git branch --show-current`
2. `main` / `master` 上なら:

```bash
git switch -c feat/{slug}
```

3. 既に作業ブランチ上なら、そのまま利用またはユーザー確認

## issue-body 連携

ブランチ slug と `docs/tasks/issue-body-{slug}.md` を一致させる（Auto open PR / Issue 起票用）。
