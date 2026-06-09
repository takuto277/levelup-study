# PRレビュー完了ラベル運用 実行計画

## 対象

- 関連 Issue: なし（ユーザー依頼によるスキル運用改善）
- 変更対象:
  - `.cursor/skills/github-pr-review/SKILL.md`
  - `.cursor/skills/github-pr-review/references/review-ok-label.md`
  - `.cursor/skills/github-pr-review/references/recheck.md`
  - `.cursor/skills/github-pr-review/references/posting-api.md`
  - `.cursor/skills/github-pr-self-review/SKILL.md`
  - `.cursor/skills/README.md`
  - `AGENTS.md`

## 背景と目的

`@codex owner/repo#123 PRレビューして` のような依頼では、レビュー結果が GitHub 上のコメントとして残る一方で、「指摘なし」または「指摘対応後に再チェックOK」になった状態がラベル一覧から見えない。

GitHub の `APPROVE` は投稿者自身の権限や運用上の誤解を生みやすいため使わず、AIレビューとして未解決の指摘がないことを示すラベルを付与する運用を追加する。

## 実装方針

1. 新規 skill は作らず、既存の `github-pr-review` に通常レビューと再チェック時のラベル運用を追加する
2. ラベル名は `review:まーじOK` とし、初回利用時に `gh label create` で作れる手順を記載する
3. 指摘なしレビュー、または再チェックでこの skill の未解決指摘がすべて解消済みになった時だけラベルを付与する
4. 指摘コメントを投稿した場合は、古いOKラベルが残らないようにラベルを外す手順を追加する
5. セルフレビューでは第三者レビュー済みと誤解されるため、このラベルを付与しない
6. README と AGENTS のスキル説明に、レビュー完了ラベル運用を追記する

## 検証計画

- `./scripts/validate-skills.sh`
- `./scripts/validate-pr.sh`
- `git diff --check`
- PR 作成後のセルフレビュー

## リスクと対応

- ラベルが GitHub approval と誤解される
  - skill に `APPROVE` 禁止とラベルの意味を明記する
- 古いOKラベルが残る
  - 指摘コメント投稿時は `review:まーじOK` を外す手順を入れる
- 自己レビューだけでOK扱いになる
  - `github-pr-self-review` ではラベル付与を禁止する
