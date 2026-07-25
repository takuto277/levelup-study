# LevelUp Study — スキル共通補足

[u7chan/agent-skills](https://github.com/u7chan/agent-skills) 由来スキルに、このリポジトリ固有の設定を載せる。

## 品質確認（PR 前必須）

GitHub Actions は push 後のセーフティネット。**PR 作成前**にローカルで実行する:

```bash
./scripts/validate-pr.sh
./scripts/validate-pr.sh --ios   # iosApp 変更時（macOS + Xcode 26.x）
./scripts/validate-pr.sh --all   # 全領域強制
```

詳細: [AGENTS.md](../../AGENTS.md)

## GitHub 操作

- Issue / PR / レビュー / コメント返信は **`gh` / `gh api` のみ**（Actions workflow は使わない）
- PR 本文・Issue 本文は **`--body-file`** または `gh api --field body=@file`
- PR 本文テンプレの正: [github-pr-create/SKILL.md](./github-pr-create/SKILL.md)（`.github/pull_request_template.md` は廃止）

## パス

| 用途 | パス |
|------|------|
| Issue 本文 | `docs/tasks/issue-body-{slug}.md` |
| PR 本文 | `docs/tasks/pr-body-{slug}.md` |
| 実行計画書 | `docs/tasks/{YYYYMMDD}-{slug}.md` |
| Issue 起票 | `./scripts/create-issue.sh` または [github-issue-create-from-plan](./github-issue-create-from-plan/SKILL.md) |
| 機能開始 | `./scripts/feature-start.sh` |

## ブランチ命名

- `feat/{issue-num}-{slug}` または `feat/{slug}`
- `main` / `master` へ直接 push しない

## レビュー

- **PR 作成直後（必須）**: [github-pr-self-review](./github-pr-self-review/SKILL.md)
- 指摘対応: [github-pr-feedback-address](./github-pr-feedback-address/SKILL.md)
- レビュー実施: [github-pr-review](./github-pr-review/SKILL.md)（指摘あり / 再チェック待ち / OK の PRレビュー状態ラベルを更新）
- コメント返信: [github-pr-comment-reply](./github-pr-comment-reply/SKILL.md)

## スキルメンテ

- 一覧同期: [skills-readme-sync](./skills-readme-sync/SKILL.md)
- 検証: `./scripts/validate-skills.sh`
- upstream 取り込み: `./scripts/sync-upstream-skills.sh`
