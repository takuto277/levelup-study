---
name: git-commit-message
description: Conventional Commits 形式のコミットメッセージ提案（LevelUp Study モノレポ）
---

# git-commit-message

## 形式

- **英語**、命令形、≤60 文字タイトル
- プレフィックス: `feat:` `fix:` `docs:` `chore:` `test:` `refactor:`
- モノレポスコープ: `feat(backend):` `fix(mobile):` `fix(ios):` `chore(ci):`
- PR レビュー対応のみ: `fb:`（scope は対象領域）

## 本文

- 何を・なぜ（1〜2 文）

## 手順

1. `git status` / `git diff` で変更確認
2. 今回の変更だけ stage（`git add` 乱用禁止）
3. HEREDOC で commit:

```bash
git commit -m "$(cat <<'EOF'
feat(mobile): short title

Why this change was needed.

EOF
)"
```

## LevelUp 慣習

- Issue 実装 PR では Conventional Commits（`fb:` はレビュー対応時のみ）
- 日本語タイトルは避ける（CI / 履歴の一貫性）
