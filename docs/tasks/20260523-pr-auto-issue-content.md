# タスク: PR 自動 Issue 連携と説明文生成

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-05-23 |
| ステータス | 進行中 |
| Issue | （push 後 Auto open PR が起票） |

## 概要

Auto open PR を拡張し、Issue 起票・Fixes 連携・PR 3 セクション自動生成を AI 担当に統一する。

## 実装手順

1. `open-pr-on-push.yml` — `issues: write`、Issue 自動 create、本文生成
2. `pull_request_template.md` — AUTO_* プレースホルダ
3. `pr-review.mdc` / feature-delivery スキル更新
4. Mobile / iOS CI — sdk.dir・`--no-configuration-cache`

## テスト計画

- [ ] push → Issue 作成 + PR 本文に URL・3 セクション
- [ ] Mobile CI green（sdk.dir）
- [ ] iOS CI green（configuration-cache 無効）
