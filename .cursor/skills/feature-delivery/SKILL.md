---
name: feature-delivery
description: LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR）。 ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---
# Feature Delivery（Issue → PR まで）

## 2. Issue 起票（AI が実施）

```bash
# 必ず issue-body をリポジトリに置く（push 後 Actions が GitHub Issue を自動起票）
docs/tasks/issue-body-{slug}.md
```

`./scripts/feature-start.sh` でも可（ローカル gh + GH_TOKEN がある場合）。

## 6. PR（AI が実施 — ユーザーに Issue/説明文を書かせない）

1. 実装・validate 完了後 `git push`
2. **Auto open PR** が Issue 起票 + `Fixes #n` + 説明文 3 セクション + Issue URL を自動生成
3. **ユーザー向けチェックリスト**は CI / レビュー / 手元確認（Secrets 等）のみ
4. `issue-body` が無い瑣末な変更は Issue なし PR でよい
5. ルール: `.cursor/rules/pr-review.mdc`
6. **マージはユーザー**

## 禁止事項

- ユーザー明示なしの commit / push / merge
- PR 本文を空のまま残す（push 前に issue-body を整備すること）
- シークレットのコミット
