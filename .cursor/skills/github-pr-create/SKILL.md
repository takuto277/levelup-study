---
name: github-pr-create
description: >
  LevelUp Study で PR を作成・更新するとき。`PRまで`、`pushしてPR`、`gh pr create` 依頼時。
  PR 前に validate-pr.sh を実行し、本文はこのスキルのテンプレート + --body-file で渡す。
---

# github-pr-create（LevelUp Study）

`gh` CLI で PR を作成・更新する。**PR 本文テンプレはこのスキルが正**（`.github/pull_request_template.md` は Auto open PR 用のレガシー）。

## 事前確認

- `gh auth status` が成功すること
- `./scripts/validate-pr.sh`（必要なら `--ios`）を **push 前** に実行済みであること
- 作業ブランチ上にいること（`main` / `master` 直 push 禁止）
- PR 本文は **ファイル経由**（`--body-file`）。シェル直埋め込み禁止

## 1. 品質チェック（未実施なら必須）

```bash
./scripts/validate-pr.sh
# iosApp/** を触った場合（macOS）:
./scripts/validate-pr.sh --ios
```

追加コマンドは [AGENTS.md](../../AGENTS.md) を参照。

失敗時は修正して再実行。直せない場合はユーザーに報告して中断。

## 2. PR 本文テンプレート

`docs/tasks/pr-body-{branch-slug}.md` に生成し、`gh pr create --body-file` で渡す。

```markdown
Fixes #{issue_number}

## Issues

- Close #{issue_number}

## Why

変更が必要な背景（`docs/tasks/issue-body-*.md` の背景・目的から要約）。

## Summary

2〜3 文でこの PR の要約。

## Changes

- 変更点 1
- 変更点 2

## Verification

validate-pr.sh の実行結果を列挙:

- `./scripts/validate-pr.sh` — passed
- （実行したコマンドと結果）

## Checklist（エージェント実施分）

- [x] `./scripts/validate-pr.sh`
- [x] 変更範囲の lint / test
- [ ] （該当時）手元 UI 確認 — ユーザー

## ユーザーがやるべきこと（マージ前）

- [ ] **CI** — Checks タブ green（push 後のセーフティネット）
- [ ] **レビュー依頼**
- [ ] **（該当時）手元確認** — 実機・Render・Secrets 等
```

Issue 番号が無い場合は `Fixes` 行と `Issues` セクションを省略。

## 3. PR 作成

```bash
BRANCH=$(git branch --show-current)
BASE="${BASE_BRANCH:-main}"
BODY_FILE="docs/tasks/pr-body-${BRANCH//\//-}.md"

# 未 push なら
git push -u origin HEAD

gh pr create \
  --base "$BASE" \
  --title "タイトル（issue-body の # Issue: 行から）" \
  --body-file "$BODY_FILE"

gh pr view "$BRANCH" --json title,body,url
```

既存 PR がある場合:

```bash
gh pr edit "$BRANCH" --body-file "$BODY_FILE"
# 失敗時: gh api repos/{owner}/{repo}/pulls/{number} --method PATCH --field body=@"$BODY_FILE"
```

## Auto open PR との関係

- push 時の **Auto open PR** は Issue 起票 + 初回本文生成を担当しうる
- エージェントは validate 結果を **`Verification` / `Checklist` に反映**するため、必要なら `gh pr edit --body-file` で上書き同期する
- **CI green はユーザー確認項目**（エージェントは push 前 validate を正とする）

## エラー対応

- `gh` 未認証 → `gh auth login`
- validate 失敗 → 修正してから PR
- Auto open PR が `Fixes #` を誤る → `gh pr edit` で修正
