---
name: skills-readme-sync
description: >
  Use this when adding, renaming, removing, or materially changing skills under
  .cursor/skills/ and the README may need to be updated. Synchronizes
  .cursor/skills/README.md Available Skills with the current skill set.
---

# Skills README Sync（LevelUp Study）

## 概要

`.cursor/skills/` 内のスキル変更に合わせて [README.md](../README.md) の `Available Skills` を同期する。

## 使用タイミング

- 新しいスキルディレクトリを追加した時
- 既存スキルの名前を変更 / 削除した時
- `SKILL.md` の説明変更により README の説明文も見直すべき時

## 手順

1. `find .cursor/skills -name SKILL.md | sort` で実在スキルを確認
2. [README.md](../README.md) のグループ別表を更新（[agent-skills のグルーピング方針](https://github.com/u7chan/agent-skills) に準拠）
3. LevelUp 専用スキルは **LevelUp 専用** グループに置く
   - `feature-delivery`, `project-context`, `github-pr-self-review`
4. `./scripts/validate-skills.sh` で README 整合を確認

## グループ（LevelUp）

| グループ | 例 |
|---------|-----|
| LevelUp 専用 | feature-delivery, project-context, github-pr-self-review |
| Git ローカル操作 | git-* |
| GitHub Issue / PR | github-issue-*, github-pr-*（create → self-review → feedback → review → reply の流れ順） |
| 実装 / 成果物生成 | github-implement-pr, html-artifact-format |
| 要件定義 / 設計対話 | grill-* |
| 品質 / テスト設計 | qa-test-design |
| スキル作成 / メンテナンス | skill-author, skills-readme-sync |

## 品質チェック

- [ ] 全 SKILL.md が README に載っている
- [ ] リンク先パスが `.cursor/skills/` からの相対パスと一致
- [ ] `./scripts/validate-skills.sh` が通る
