## Issues

- Close #69

## 背景

PRレビュー依頼は `@codex #123 のPRレビューして` のような自然文で来る想定があるため、既存の `github-pr-review` skill が対象 PR を迷わず特定できるようにします。あわせて、指摘がない場合も PR 上に review comment を残し、レビュー済みであることを GitHub 上で確認できるようにします。

## 概要

既存の `github-pr-review` skill を拡張し、PR URL・`owner/repo#number`・PR番号・ブランチ名・現在ブランチから対象 PR を解決する手順を追加しました。指摘なしの場合は `APPROVE` ではなく `COMMENT` review を投稿するルールと API 例も追加しています。

## 変更内容

- `github-pr-review` の起動条件に `@codex #123 のPRレビューして` 形式の依頼を追加
- 対象 PR の解決順序と、指摘なし review comment の投稿手順を追加
- posting rules / posting API にコメント言語、重要度ラベル、指摘なしコメントの扱いを追加
- `.cursor/skills/README.md` と `AGENTS.md` の skill 説明を更新
- Issue 本文と実行計画書を `docs/tasks/` に追加

## 確認項目

- [x] `./scripts/validate-skills.sh`
- [x] `./scripts/validate-pr.sh`
- [x] `git diff --check origin/main...HEAD`
- [x] PR 作成後のセルフレビュー（[must] なし）
