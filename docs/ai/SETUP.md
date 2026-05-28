# AI エージェントセットアップ（LevelUp Study）

コーディングエージェント（Cursor / Claude Code 等）がこのリポジトリで機能開発するためのセットアップ手順。

## スキルの置き場

| ツール | パス | 備考 |
|--------|------|------|
| Cursor | `.cursor/skills/` | **正本**（リポジトリ内に vendor） |
| Claude Code | `.claude/skills/` | 一部ミラー。詳細は `.rulesync/` |
| 共通補足 | `.cursor/skills/LEVELUP.md` | LevelUp 固有ルール |
| エージェント入口 | `AGENTS.md` | 検証コマンド・gh 運用 |

ベーススキル: [u7chan/agent-skills](https://github.com/u7chan/agent-skills)

## 必須ツール

```bash
# GitHub CLI（Issue / PR / レビュー）
gh auth login
gh auth status

# PR 前検証
./scripts/validate-pr.sh

# スキル変更時
./scripts/validate-skills.sh
```

## 標準フロー

1. Issue 起票 — `github-issue-create-from-plan` または `./scripts/feature-start.sh`
2. 実装 — `github-implement-pr` / `feature-delivery`
3. 検証 — `./scripts/validate-pr.sh`（iosApp 変更時 `--ios`）
4. PR 作成 — `github-pr-create` + `gh pr create --body-file`
5. **セルフレビュー（必須）** — [github-pr-self-review](../../.cursor/skills/github-pr-self-review/SKILL.md)
6. ユーザーが CI green・レビュー依頼・マージ

**PR 作成後にセルフレビューを省略して「完了」と報告しない。**

## upstream 同期（agent-skills）

```bash
./scripts/sync-upstream-skills.sh --dry-run   # 差分確認
./scripts/sync-upstream-skills.sh             # 取り込み（LEVELUP 節は手動確認）
./scripts/validate-skills.sh
```

取り込み後は `LEVELUP.md` 追記部分が消えていないか、`github-pr-self-review` など LevelUp 専用スキルが残っているか確認する。

## 参考

- [AGENTS.md](../../AGENTS.md)
- [.cursor/skills/README.md](../../.cursor/skills/README.md)
- [u7chan/monorepo](https://github.com/u7chan/monorepo) — symlink + 変更検知 CI の運用例
