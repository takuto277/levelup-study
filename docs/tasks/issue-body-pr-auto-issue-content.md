# Issue: PR 作成時の Issue 自動起票と説明文自動生成

## 背景

PR テンプレの「ユーザーがやるべきこと」に Issue 連携や説明文記入が含まれており、実装担当（AI）がやるべき作業がユーザーに押し付けられていた。

## 目的

- `docs/tasks/issue-body-{slug}.md` があるブランチは **push 時に Issue を自動起票**（既存があれば再利用）
- PR 本文の **なぜ / 何が / どうなる** を issue-body・実行計画書・git diff から **Actions が自動生成**
- Issue の **GitHub URL** と **リポジトリ内 issue-body へのリンク** を PR に記載
- ユーザーのチェックリストは **CI・レビュー・手元確認（該当時）** のみ
- `issue-body` が無いブランチは Issue なし PR（Fixes 行も省略）

## 受け入れ条件

- [ ] `open-pr-on-push.yml` が Issue 未存在時に `issues.create` する
- [ ] PR 本文 3 セクションがプレースホルダではなく内容付きで生成される
- [ ] PR に Issue URL と issue-body ファイル URL が含まれる
- [ ] `.cursor/rules/pr-review.mdc` / feature-delivery スキルが AI 担当を明記
- [ ] Mobile / iOS CI の既知失敗（SDK・configuration-cache）を修正
