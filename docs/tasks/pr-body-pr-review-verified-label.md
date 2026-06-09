## Issues

- なし（ユーザー依頼によるスキル運用改善）

## 背景

PRレビュー依頼で指摘なし、またはレビュー指摘の修正後に再チェックOKになった状態を、GitHub のラベル一覧から判別できるようにしたい。

GitHub の approval はレビュー投稿者や権限の扱いで誤解を生みやすいため使わず、既存の `github-pr-review` skill に `review:まーじOK` ラベル運用を追加する。

## 概要

`github-pr-review` に、指摘なしレビューまたは再チェックOK時に `review:まーじOK` ラベルを付与する手順を追加した。指摘コメントを投稿した場合は古いOKラベルを外し、セルフレビューではこのラベルを付与しないようにした。

## 変更内容

- `github-pr-review` に `review:まーじOK` ラベル更新ステップを追加
- ラベルの付与条件、除去条件、`gh` コマンド例を `review-ok-label.md` に分離
- 再チェック手順に、全解消時のラベル付与と未解決時のラベル除去を追加
- セルフレビューでは `review:まーじOK` を付与しないルールを追加
- `AGENTS.md` と skill README / LEVELUP の説明を更新
- 実行計画書を追加

## 確認項目

- [x] `./scripts/validate-skills.sh`
- [x] `git diff --check`
- [x] `./scripts/validate-pr.sh`（docs / skills 差分のため対象チェックなしで正常終了）

## 詳細

ラベル名は `review:まーじOK`。これは GitHub approval ではなく、AIレビューとして未解決の指摘がない状態を見える化するための運用ラベル。
