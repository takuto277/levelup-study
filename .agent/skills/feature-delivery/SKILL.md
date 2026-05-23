---
name: feature-delivery
description: >-
  LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR）。
  ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
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

`docs/tasks/issue-body-{slug}.md` を置くと、後続の Auto open PR が Issue 番号を自動連携しやすくなる。

## 3. ブランチ

```bash
git checkout -b feat/{slug}
```

## 4. 実装 → validate

変更内容に応じて validate を実行（例: `./scripts/assets/run.sh scripts/assets/validate_assets.py`）。

## 5. push

```bash
git push -u origin HEAD
```

## 6. PR（重要）

- push 後 **Auto open PR** が走り、オープン PR が無ければ **通常 PR**（ドラフトではない）を作成
- 本文テンプレ: `.github/pull_request_template.md`
  - **なぜやったのか / 何が変わったのか / どうなるのか**
  - **ユーザーがやるべきこと** — レビュー依頼前のチェックリスト（Actions が自動生成）
- **`Fixes #n`**: `docs/tasks/issue-body-{slug}.md` があるブランチは Actions が **自動挿入**（毎回手動で書く必要はない）
- ルール: `.cursor/rules/pr-review.mdc`
- **マージはユーザーが実施**（AI は明示指示がない限り commit / push / merge しない）

## アセット変更時

`docs/assets/01_Asset_Ingestion_Workflow.md` と `assets` ルールに従い sync / validate を実行。

## 禁止事項

- ユーザー明示なしの commit / force push / マージ
- シークレットのコミット
