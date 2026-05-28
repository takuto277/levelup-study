# agent skills（LevelUp Study）

コーディングエージェント用スキル集。[u7chan/agent-skills](https://github.com/u7chan/agent-skills) をベースに LevelUp 向け補足を載せています。

LevelUp 固有設定: [LEVELUP.md](./LEVELUP.md)  
エージェント向けリファレンス: [AGENTS.md](../AGENTS.md)

## Available Skills

### LevelUp 専用

| Skill | Description |
|-------|-------------|
| [feature-delivery](feature-delivery/SKILL.md) | Issue → 実装 → validate → PR → セルフレビュー → レビュー対応の標準フロー |
| [project-context](project-context/SKILL.md) | 本リポジトリの制約サマリ |

### Git ローカル操作

| Skill | Description |
|-------|-------------|
| [git-branch-create](git-branch-create/SKILL.md) | ブランチ名を提案し、ブランチを作成する |
| [git-worktree-create](git-worktree-create/SKILL.md) | 独立した git worktree を作成し、並列作業用の作業領域を用意する |
| [git-commit-message](git-commit-message/SKILL.md) | コミットメッセージを提案する |

### GitHub Issue / PR

| Skill | Description |
|-------|-------------|
| [github-issue-create-from-plan](github-issue-create-from-plan/SKILL.md) | 設計プラン合意後に GitHub Issue を作成する |
| [github-pr-create](github-pr-create/SKILL.md) | PR 本文生成を含めて GitHub に PR を作成する |
| [github-pr-self-review](github-pr-self-review/SKILL.md) | PR 作成直後に必須のセルフレビュー（完了報告前） |
| [github-pr-feedback-address](github-pr-feedback-address/SKILL.md) | GitHub PR のレビュー指摘を確認し、実装対応から返信まで行う |
| [github-pr-review](github-pr-review/SKILL.md) | 指定した GitHub PR をレビューし、FB 対応後の再チェックまで行う |
| [github-pr-comment-reply](github-pr-comment-reply/SKILL.md) | GitHub PR の review comment や conversation comment に返信する |

### 実装

| Skill | Description |
|-------|-------------|
| [github-implement-pr](github-implement-pr/SKILL.md) | 既存スキルを参照しつつ Issue 確認から PR 作成まで進める |
| [html-artifact-format](html-artifact-format/SKILL.md) | AI向けMarkdownと人間向けHTMLを判断し、視覚化要素入りの単一HTMLを生成する |

### 要件定義 / 設計対話

| Skill | Description |
|-------|-------------|
| [grill-me](grill-me/SKILL.md) | 計画や設計を一問ずつ厳しく掘り下げ、意思決定の曖昧さを解消する |
| [grill-with-docs](grill-with-docs/SKILL.md) | 既存ドキュメントやコードと照合しながら設計を詰め、用語集やADRを必要に応じて更新する |

### 品質 / テスト設計

| Skill | Description |
|-------|-------------|
| [qa-test-design](qa-test-design/SKILL.md) | QA観点とテストケースを体系的に洗い出し、テスト実装前の設計を整える |

### スキル作成 / メンテナンス

| Skill | Description |
|-------|-------------|
| [skill-author](skill-author/SKILL.md) | SKILL.md ファイルの作成と改善を行う |
| [skills-readme-sync](skills-readme-sync/SKILL.md) | **本リポジトリ専用** — README のスキル一覧を現在の構成へ同期する |

## 方針

- **Issue / PR / レビュー**: `gh` CLI + 上記スキル（GitHub Actions workflow は使わない）
- **PR 作成後**: 必ず `github-pr-self-review` を実行してから完了報告
- **CI（Actions）**: lint / test + skills-ci（`.cursor/skills/` 変更時）
- **PR 本文テンプレ**: `github-pr-create` スキル内（`.github/pull_request_template.md` は廃止）

## セットアップ

[docs/ai/SETUP.md](../../docs/ai/SETUP.md)

## upstream 同期

```bash
./scripts/sync-upstream-skills.sh --dry-run
./scripts/sync-upstream-skills.sh
./scripts/validate-skills.sh
```

手動の場合:

```bash
git clone https://github.com/u7chan/agent-skills /tmp/u7chan-agent-skills
# 必要スキルを .cursor/skills/ にコピーし、LEVELUP.md との整合を確認
```
