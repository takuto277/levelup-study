---
name: feature-delivery
description: LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR）。 ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---
# Feature Delivery（Issue → PR まで）

入口の詳細手順は [github-implement-pr](../github-implement-pr/SKILL.md) も参照。

## 1. Issue 起票

```bash
docs/tasks/issue-body-{slug}.md
```

`./scripts/feature-start.sh` でも可（ローカル gh + GH_TOKEN がある場合）。

## 2. 実装

- [project-context](../project-context/SKILL.md) で制約確認
- 3 ステップ以上 → `docs/tasks/` に実行計画書

## 3. validate（PR 前・必須）

**GitHub Actions に任せない。** push 前にローカルで実行:

```bash
./scripts/validate-pr.sh
# iosApp 変更時（macOS）:
./scripts/validate-pr.sh --ios
```

詳細: [AGENTS.md](../../../AGENTS.md)

画面/API 変更時は [qa-test-design](../qa-test-design/SKILL.md) で手動確認項目も列挙。

## 4. コミット

[git-commit-message](../git-commit-message/SKILL.md) に従う。

## 5. PR

1. `git push -u origin HEAD`
2. PR 本文は [github-pr-create](../github-pr-create/SKILL.md) のテンプレで `docs/tasks/pr-body-{slug}.md` を生成
3. `gh pr create --body-file ...` または Auto open PR 後に `gh pr edit` で Verification を同期
4. **CI** は push 後のセーフティネット（ユーザー Checklist 項目）
5. ルール: [pr-review.mdc](../../rules/pr-review.mdc)
6. **マージはユーザー**

## 禁止事項

- validate 未実行で PR 完了報告
- ユーザー明示なしの commit / push / merge
- PR 本文空・Verification なし
- シークレットのコミット
