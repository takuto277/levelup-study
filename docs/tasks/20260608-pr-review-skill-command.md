# PRレビュー依頼用 skill トリガー強化 実行計画

## 対象

- 関連 Issue: #69
- 変更対象:
  - `.cursor/skills/github-pr-review/SKILL.md`
  - `.cursor/skills/github-pr-review/references/posting-rules.md`
  - `.cursor/skills/github-pr-review/references/posting-api.md`
  - `.cursor/skills/README.md`
  - `AGENTS.md`

## 背景と目的

`github-pr-review` は既に存在するが、実際の依頼は `@codex #123 のPRレビューして` のように自然文で来る。PR番号や URL だけでなく、`owner/repo#number`、ブランチ名、現在ブランチから対象 PR を解決する手順を skill に明記しておくことで、エージェントが都度迷わずレビューに入れる。

加えて、指摘がない場合も PR 上へ `COMMENT` review を残すことで、依頼者が「レビュー済み」を GitHub 上で確認できる。作成者本人が approval する形は避けるため、`APPROVE` は引き続き禁止する。

## 実装方針

1. 既存の `github-pr-review` skill を拡張し、新規 skill は作らない
2. description と使用タイミングに自然文トリガーを追加する
3. PR 特定手順を `PR URL → owner/repo#number → #number/number → branch → current branch` の順に整理する
4. 指摘なし時の `COMMENT` review 投稿ルールを skill 本文、posting rules、posting API 例へ追加する
5. skill 一覧と `AGENTS.md` の説明を更新する
6. `./scripts/validate-skills.sh` と `./scripts/validate-pr.sh` で確認する

## 検証計画

- `./scripts/validate-skills.sh`
- `./scripts/validate-pr.sh`
- `git diff --check`
- PR 作成後のセルフレビュー

## リスクと対応

- description が長くなりすぎると skill の起動条件が読みづらい
  - 代表例だけを入れ、詳細な解決順序は本文に寄せる
- 指摘なしコメントが approval と誤解される
  - `APPROVE` 禁止と `COMMENT` review を明記し、本文例も approval 表現を避ける
- 外部参考リポジトリを誤って編集する
  - `u7chan/agent-skills` / `u7chan/monorepo` は `/private/tmp` で読むだけにし、PR は LevelUp Study にだけ出す
