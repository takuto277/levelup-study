## Issues

- Close #40

## Why

[u7chan/agent-skills](https://github.com/u7chan/agent-skills) をベースに `.cursor/skills/` を全面更新し、**Issue / PR / レビューをすべて `gh` CLI + スキル**に統一する。GitHub Actions は lint/test のみに限定し、Auto open PR / create-issue workflow / PR テンプレートを廃止する。

## Summary

- u7chan/agent-skills 由来スキル 14 種を `.cursor/skills/` に配置
- `LEVELUP.md` / `AGENTS.md` / `feature-delivery` で LevelUp 固有フローを定義
- `.github/workflows/open-pr-on-push.yml`, `create-issue.yml`, `pull_request_template.md` を削除
- README / 開発フロー doc / pr-review ルールを gh CLI 前提に更新

## Changes

- `.cursor/skills/` — github-implement-pr, github-pr-create, github-pr-review, github-pr-feedback-address, github-issue-create-from-plan, git-worktree-create, grill-me, grill-with-docs, qa-test-design 等
- `.cursor/skills/LEVELUP.md`, `.cursor/skills/README.md` — 一覧と LevelUp 補足
- `.cursor/skills/feature-delivery/SKILL.md` — gh CLI フロー完結
- `AGENTS.md`, `.cursor/rules/pr-review.mdc`, `CLAUDE.md`, `README.md` — Actions 依存の記述削除
- `scripts/feature-start.sh` — Auto open PR 言及削除
- `docs/architecture/02_Development_Workflow.md`, `docs/assets/01_Asset_Ingestion_Workflow.md`

## Verification

- `./scripts/validate-pr.sh --all` — passed
- `./scripts/validate-pr.sh` — scripts 変更時 shell syntax check、docs のみ時はスキップメッセージを表示

## Checklist（エージェント実施分）

- [x] `./scripts/validate-pr.sh --all`
- [x] スキル・ドキュメント整合
- [x] Auto open PR / create-issue workflow 削除

## ユーザーがやるべきこと（マージ前）

- [ ] **CI** — Checks タブ green
- [ ] **レビュー依頼**
