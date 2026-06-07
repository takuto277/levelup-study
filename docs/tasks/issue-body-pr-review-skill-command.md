# Issue: PRレビュー依頼用 skill トリガー強化

## 背景

LevelUp Study には既に `.cursor/skills/github-pr-review` があり、GitHub PR の差分確認とコメント投稿を行える。ただし、実運用では `@codex #123 のPRレビューして`、`@codex owner/repo#123 をレビューして`、`feature/foo のPRレビューして` のように、bot mention・自然文・PR番号・ブランチ名が混ざった依頼が想定される。

また、レビュー結果として指摘がない場合も、作業済みであることが PR 上に残らないと依頼者が状態を確認しづらい。PR 作成者本人として review を投稿するため approval は避けつつ、`COMMENT` review として指摘なしコメントを残す運用を明文化したい。

## 目的

- `@codex 〇〇のPRレビューして` のような自然文依頼から対象 PR を特定できるようにする
- レビューコメントの投稿を `gh` CLI / `gh api` に統一し、GitHub コネクタへ依存しない
- 指摘がない場合も `APPROVE` ではなく `COMMENT` review として PR 上へ結果を残す
- PRレビュー、指摘なし、FB再チェックの一連の手順を skill と参照ドキュメントで追える状態にする

## スコープ

- `.cursor/skills/github-pr-review/SKILL.md`
  - skill 起動条件に `@codex #123 のPRレビューして` 形式を追加
  - PR URL、`owner/repo#number`、PR番号、ブランチ名、現在ブランチの順で対象 PR を特定する手順を追加
  - 指摘なし review comment の投稿手順を追加
- `.cursor/skills/github-pr-review/references/posting-rules.md`
  - コメント言語、重要度ラベル、指摘なしコメントのルールを追加
- `.cursor/skills/github-pr-review/references/posting-api.md`
  - `COMMENT` review の payload 例と指摘なし review comment の例を追加
- `.cursor/skills/README.md` / `AGENTS.md`
  - skill 一覧の説明を現行運用に合わせて更新

## スコープ外

- GitHub Actions workflow の追加・変更
- `u7chan/agent-skills` / `u7chan/monorepo` 側の変更
- 実際の PR レビュー自動実行 bot の常駐化
- `gh` 認証情報の管理

## 受け入れ条件

- [ ] `github-pr-review` の description だけを読んで、`@codex #123 のPRレビューして` 形式で起動すべきことが分かる
- [ ] PR URL、`owner/repo#number`、PR番号、ブランチ名、現在ブランチから対象 PR を特定する手順が明文化されている
- [ ] 指摘がある場合は inline review comment を優先し、差分行に付けられない指摘だけ overall review comment に落とす
- [ ] 指摘がない場合は `APPROVE` ではなく `COMMENT` review として指摘なしコメントを投稿する
- [ ] コメント本文はファイルまたは JSON 入力で渡し、Markdown をシェル引数へ直接埋め込まない
- [ ] `./scripts/validate-skills.sh` が通る
