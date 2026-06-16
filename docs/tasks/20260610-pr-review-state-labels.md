# PRレビュー状態ラベル運用 実行計画

## 対象

- 関連 Issue: なし（ユーザー依頼によるスキル運用改善）
- 変更対象:
  - `.cursor/skills/github-pr-create/SKILL.md`
  - `.cursor/skills/github-pr-review/SKILL.md`
  - `.cursor/skills/github-pr-review/references/review-state-labels.md`
  - `.cursor/skills/github-pr-review/references/recheck.md`
  - `.cursor/skills/github-pr-review/references/posting-api.md`
  - `.cursor/skills/github-pr-feedback-address/SKILL.md`
  - `.cursor/skills/github-pr-self-review/SKILL.md`
  - `.cursor/skills/README.md`
  - `.cursor/skills/LEVELUP.md`
  - `AGENTS.md`

## 背景と目的

PRレビュー運用で、レビュー待ち・修正待ち・指摘なし/再チェックOK の状態が GitHub の PR 一覧から分かりづらい。

GitHub approval は使わず、運用ラベルで状態を見える化する。

## 方針

次の 3 ラベルを排他的に扱う。

| Label | 意味 |
|------|------|
| `PRレビュー/再レビュー待ち` | 初回レビュー、またはレビュー指摘対応後の再レビュー待ち |
| `PRレビュー修正待ち` | PRレビュー済みで、修正・回答・追加対応が必要 |
| `review:まーじOK` | AIレビューとして未解決の指摘がない |

状態遷移:

- PR 作成直後: `PRレビュー/再レビュー待ち`
- レビューで指摘あり: `PRレビュー修正待ち`
- レビューで指摘なし: `review:まーじOK`
- FB 対応完了後: `PRレビュー/再レビュー待ち`
- 再チェック OK: `review:まーじOK`
- 再チェックで未解決あり: `PRレビュー修正待ち`

## 検証計画

- `./scripts/validate-skills.sh`
- `git diff --check`
- GitHub label が存在することを `gh label list` で確認
- open PR に状態ラベルを試験反映する
