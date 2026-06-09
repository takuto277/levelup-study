---
name: github-pr-self-review
description: >
  PR 作成直後に必ず実行するセルフレビュー。実装者エージェントが自分の PR を github-pr-review と同手順でチェックし、
  [must] は修正して push、[should]/[nit] は PR にコメントする。ユーザーへ「PR 完了」報告の前に必須。
---

# 概要

**PR 作成の直後、ユーザーへ完了報告する前に必ず実行する。**

中身のレビュー手順は [github-pr-review](../github-pr-review/SKILL.md) と同一。セルフレビュー固有のルールだけここに載せる。

# いつ実行するか

- `gh pr create` 直後（[github-pr-create](../github-pr-create/SKILL.md) の完了ステップ）
- [github-implement-pr](../github-implement-pr/SKILL.md) の「PR 作成」工程の直後
- [feature-delivery](../feature-delivery/SKILL.md) ステップ 5b の直後

# セルフレビュー固有ルール

1. **`gh auth status` が成功していること**（未認証なら `gh auth login` を促し、セルフレビュー未完了として報告）
2. **APPROVE しない**（github-pr-review と同じ）
3. **`review:まーじOK` ラベルを付与しない**（通常レビュー・再チェックOKを示すラベルであり、セルフレビュー完了ラベルではない）
4. **`[must]` がある場合**
   - ユーザーへ「マージ可」と報告しない
   - 修正 → `./scripts/validate-pr.sh` → commit → push → セルフレビューを再実行
5. **`[should]` / `[nit]`**
   - GitHub PR に inline コメントとして残す（チャットだけに留めない）
   - マージ判断はユーザーに委ねる旨を最終報告に書く
6. **指摘ゼロでも**「セルフレビュー実施済み・[must] なし」を最終報告に明記する

# 手順（github-pr-review へ委譲）

1. 作成した PR 番号 / URL を対象に [github-pr-review](../github-pr-review/SKILL.md) の Step 1〜7 を実行
2. レビュー観点: [review-criteria.md](../github-pr-review/references/review-criteria.md)
3. コメント投稿: [posting-rules.md](../github-pr-review/references/posting-rules.md)

# 最終報告テンプレ

```markdown
## セルフレビュー結果（PR #n）
- [must]: 0 件（または N 件 → 修正済み / 要対応）
- [should]/[nit]: M 件（PR にコメント済み）
- CI: （Checks タブの状態）
- マージ判断: ユーザー確認待ち
```
