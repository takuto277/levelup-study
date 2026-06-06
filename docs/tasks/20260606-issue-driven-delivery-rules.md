# Issue Driven Development ルール追加 実行計画

## 対象

- 関連 Issue: #40
- 新規起票 Issue: #49, #50, #51

## 背景と目的

今後の LevelUp Study では、GitHub Issue を要件定義として扱い、実装前に実行計画書・設計を作ってからコード変更へ進む運用に寄せる。現状も `AGENTS.md` や `.cursor/skills/` に近いルールはあるが、Issue を正本にすること、計画書に含めるべき内容、PR 作成後のセルフレビューまでの標準フェーズが分散している。

あわせて、現在のアプリを見て既存 Issue と重複しない不足機能を追加 Issue として整理する。

## 今回のスコープ

- Issue Driven Development の標準フェーズを `docs/ai/` に追加する
- `AGENTS.md`、Cursor rules、rulesync rules、関連 skill から標準フェーズを参照する
- アプリ確認から見えた不足機能の Issue を起票し、本文を `docs/tasks/` に残す
- ロードマップへ新規 Issue と直近完了 Issue を反映する

## スコープ外

- #49, #50, #51 の実装
- `task-init` コマンド自体の改修
- GitHub Actions の変更
- 既存 skill 群の大規模再編

## 既存コード・ドキュメント調査

- `AGENTS.md` は validate / gh CLI / PR body file の方針を持つが、Issue を要件定義の正本にする記述は弱い
- `.cursor/rules/always.mdc` と `.rulesync/rules/always.md` は 3 ステップ以上の計画書作成を示すが、Issue から始める順序が明文化されていない
- `.cursor/skills/feature-delivery` と `.cursor/skills/github-implement-pr` は Issue → PR の入口だが、計画書に含める項目が不足している
- モバイルは初回オンボーディング、未同期状態の可視化、ユーザー定義目標ボーナスが既存 open Issue と重複しない不足領域として見える

## 実装手順

1. 既存 open / closed Issue を確認し、重複しない不足機能を選ぶ
2. 新規 Issue 本文を `docs/tasks/issue-body-*.md` に作成し、`gh issue create --body-file` で起票する
3. `docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md` を追加する
4. `AGENTS.md`、rules、skill に Issue → 計画 → 実装 → 検証 → PR の順序を追加する
5. `docs/planning/01_Features_and_Roadmap.md` に新規 Issue を反映する
6. skill 変更のため `./scripts/validate-skills.sh` を実行する

## 検証計画

- `./scripts/validate-skills.sh`
- `./scripts/validate-pr.sh`
- PR 作成後にセルフレビューを行い、`[must]` があれば修正する

## リスクと互換性

- ドキュメント・ルール変更のみで、アプリや API のランタイム挙動は変えない
- ルールが重複して読みにくくならないよう、詳細は `docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md` に集約し、他ファイルは参照と要点に留める
- `.rulesync/rules/always.md` と `.cursor/rules/always.mdc` の内容がずれると運用に混乱が出るため、同じ内容で更新する
