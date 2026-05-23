---
name: feature-delivery
description: LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR）。 ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---
# Feature Delivery（Issue → PR まで）

ユーザーが毎回口頭でフローを指示しなくてよいよう、以下を **この順序で** 実行する。

## 1. コンテキスト把握

1. `project-context` スキルの要約を確認
2. 関連する `docs/features/` / `docs/tasks/` / `docs/assets/` を読む
3. 3 ステップ以上の変更なら `docs/tasks/YYYYMMDD-<slug>.md` 実行計画書を作成

## 2. Issue 起票

```bash
./scripts/create-issue.sh --title "feat: 短いタイトル" --body-file docs/tasks/issue-body-xxx.md
```

または `./scripts/feature-start.sh` で Issue + ブランチを一括作成。

## 3. 実装 → validate → push → ドラフト PR 自動作成

詳細は `.cursor/skills/feature-delivery/SKILL.md`（rulesync 生成後は同一内容）。

## アセット変更時

`docs/assets/01_Asset_Ingestion_Workflow.md` と `assets` ルールに従い sync / validate を実行。
