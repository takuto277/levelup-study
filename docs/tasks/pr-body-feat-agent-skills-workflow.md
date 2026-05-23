## Issues

- （Auto open PR 起票後に Close #n に更新）

## Why

友人の agent-skills 方式を LevelUp Study に取り込み、**push 前のローカル validate** と **PR 本文テンプレのスキル化** を標準にする。GA はセーフティネットとして維持。

## Summary

`AGENTS.md` と `validate-pr.sh` を追加し、`.cursor/skills/` に github-pr-create 等を配置。feature-delivery / pr-review を更新。

## Changes

- `AGENTS.md` — エージェント向け validate コマンド一覧
- `scripts/validate-pr.sh` — 変更パスに応じた lint/test
- `.cursor/skills/github-pr-create` 他 — PR テンプレ・実装フロー・QA 観点
- `feature-delivery` / `pr-review.mdc` — ローカル validate 必須化

## Verification

- `./scripts/validate-pr.sh --all` — passed

## Checklist（エージェント実施分）

- [x] `./scripts/validate-pr.sh --all`
- [x] ドキュメント・スキル整合

## ユーザーがやるべきこと（マージ前）

- [ ] **CI** — Checks タブ green
- [ ] **レビュー依頼**
