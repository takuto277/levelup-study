---
name: feature-delivery
description: LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR → レビュー）。ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---

# Feature Delivery（Issue → PR → レビュー）

**Issue / PR / レビューはすべて `gh` CLI + スキル。** GitHub Actions は lint/test のみ（push 後セーフティネット）。

LevelUp 固有設定: [LEVELUP.md](../LEVELUP.md)

## フロー概要

```
Plan（必要なら grill-me / grill-with-docs）
  → github-issue-create-from-plan または create-issue.sh
  → github-implement-pr（実装〜PR）
  → github-pr-review / github-pr-feedback-address（レビュー対応）
```

入口の詳細手順は [github-implement-pr](../github-implement-pr/SKILL.md)。

## 1. Issue 起票

**Actions は使わない。** 次のいずれか:

```bash
# 設計合意後（推奨）
# github-issue-create-from-plan スキルに従い docs/tasks/issue-body-{slug}.md を用意
./scripts/create-issue.sh --title "feat: ..." --body-file docs/tasks/issue-body-{slug}.md

# Issue + ブランチ + 実行計画書雛形を一括
./scripts/feature-start.sh \
  --title "feat: 機能名" \
  --body-file docs/tasks/issue-body-{slug}.md \
  --branch-slug feature-slug
```

## 2. 実装

- [project-context](../project-context/SKILL.md) で制約確認
- 3 ステップ以上 → `docs/tasks/` に実行計画書（`task-init` 相当）
- 大きい Issue は [git-worktree-create](../git-worktree-create/SKILL.md) を検討
- 画面/API 変更時は [qa-test-design](../qa-test-design/SKILL.md) で手動確認項目も列挙

## 3. validate（PR 前・必須）

```bash
./scripts/validate-pr.sh
# iosApp 変更時（macOS）:
./scripts/validate-pr.sh --ios
```

詳細: [AGENTS.md](../../../AGENTS.md)

## 4. コミット

[git-commit-message](../git-commit-message/SKILL.md) に従う。

## 5. PR 作成

1. `git push -u origin HEAD`
2. [github-pr-create](../github-pr-create/SKILL.md) のテンプレで `docs/tasks/pr-body-{slug}.md` を生成
3. `gh pr create --title "..." --body-file docs/tasks/pr-body-{slug}.md`
4. **CI** は push 後のセーフティネット（PR の Checklist にユーザー確認項目として記載）
5. **マージはユーザー**

## 5b. セルフレビュー（必須・PR 作成直後）

**ユーザーへ「PR 完了」と報告する前に必ず実行。**

[github-pr-self-review](../github-pr-self-review/SKILL.md) → 内部で [github-pr-review](../github-pr-review/SKILL.md) と同手順。

- `gh pr diff` で差分を読み、`[must]` / `[should]` / `[nit]` を判定
- `[must]` がある → 修正して push し、セルフレビューをやり直す
- `[should]` / `[nit]` → GitHub PR にコメント（APPROVE はしない）
- 指摘ゼロでも「セルフレビュー済み」を報告に含める

## 6. レビュー対応（他者レビュー・指摘対応）

- レビュー実施: [github-pr-review](../github-pr-review/SKILL.md)
- 指摘対応: [github-pr-feedback-address](../github-pr-feedback-address/SKILL.md)
- コメント返信: [github-pr-comment-reply](../github-pr-comment-reply/SKILL.md)

## 禁止事項

- validate 未実行で PR 完了報告
- **セルフレビュー未実施で PR 完了報告**
- GitHub Actions で Issue / PR を自動作成する（廃止済み）
- ユーザー明示なしの commit / push / merge
- PR 本文空・Verification なし
- シークレットのコミット
