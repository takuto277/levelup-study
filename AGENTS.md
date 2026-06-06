# AGENTS.md — LevelUp Study エージェント向けリファレンス

AI コーディングエージェント（Cursor 等）が **PR 前の品質確認** と **GitHub 操作** で参照する設定ファイル。

- **GitHub Actions**: push / PR 時の **lint / test セーフティネット**（[`.github/workflows/ci.yml`](.github/workflows/ci.yml)）
- **Issue / PR / レビュー**: **`gh` CLI + `.cursor/skills/`**（Actions workflow は使わない）
- **PR 本文テンプレ**: [`.cursor/skills/github-pr-create/SKILL.md`](.cursor/skills/github-pr-create/SKILL.md)（`.github/pull_request_template.md` は廃止）
- **開発フェーズ**: Issue を要件定義の正本として扱い、実装前に [Issue Driven Development](docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md) に沿って `docs/tasks/{YYYYMMDD}-{slug}.md` の実行計画書・設計を作る

## 検証（PR 前必須）

```bash
# 変更パスに応じて backend / mobile / assets を自動実行
./scripts/validate-pr.sh

# 全部強制（macOS + Xcode ありの場合 iOS も）
./scripts/validate-pr.sh --all

# iOS xcodebuild も含める（macOS + Xcode 26.x 必須）
./scripts/validate-pr.sh --ios
```

| 領域 | コマンド | 備考 |
|------|----------|------|
| Backend | `cd backend && CGO_ENABLED=1 go test ./... -count=1 && go vet ./...` | CI と同等 |
| Mobile (KMP Android) | `cd apps/mobile && ./gradlew :shared:compileDebugKotlinAndroid` | CI と同等 |
| Mobile (KMP iOS link) | `cd apps/mobile && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` | CI ios job 前半 |
| iOS (Swift) | `cd apps/mobile && xcodebuild ...` | **ローカル macOS のみ**。CI は macos-15 + Xcode 26.3（`CI` workflow の iOS job） |
| Assets | `./scripts/assets/run.sh scripts/assets/validate_assets.py` | assets 変更時 |
| Master images | `cd backend && make master-images-validate` | master 画像変更時 |

## スキル（`.cursor/skills/`）

ベース: [u7chan/agent-skills](https://github.com/u7chan/agent-skills)。LevelUp 補足: [LEVELUP.md](.cursor/skills/LEVELUP.md)

| スキル | 用途 |
|--------|------|
| `feature-delivery` | LevelUp 標準フロー（Issue → 実装 → validate → PR → レビュー） |
| `github-implement-pr` | Issue 実装〜PR までの入口 |
| `github-issue-create-from-plan` | 設計合意後の Issue 起票 |
| `github-pr-create` | PR 本文テンプレ + `gh pr create --body-file` |
| `github-pr-self-review` | **PR 作成直後の必須セルフレビュー**（完了報告前） |
| `github-pr-review` | PR レビュー |
| `github-pr-feedback-address` | レビュー指摘の実装対応 |
| `github-pr-comment-reply` | PR コメント返信 |
| `qa-test-design` | 手動 UI 確認含むテスト観点 |
| `git-branch-create` / `git-worktree-create` / `git-commit-message` | Git 操作 |
| `grill-me` / `grill-with-docs` | 要件・設計の詰め |
| `project-context` | 本リポジトリの制約サマリ |
| `skills-readme-sync` | `.cursor/skills/README.md` とスキル一覧の同期 |

## スキル検証（`.cursor/skills/` 変更時）

```bash
./scripts/validate-skills.sh
```

セットアップ: [docs/ai/SETUP.md](docs/ai/SETUP.md)

## GitHub CLI

- Issue: `gh issue create --body-file ...` または `./scripts/create-issue.sh`
- PR: `gh pr create --body-file ...`（[github-pr-create](.cursor/skills/github-pr-create/SKILL.md)）
- レビュー: `gh pr review`, `gh api`（[github-pr-review](.cursor/skills/github-pr-review/SKILL.md)）
- **GitHub コネクタは使わない**（403 になりやすい）
- 本文は **`--body-file`** または `gh api --field body=@file`（シェル直埋め込み禁止）
- **PR タイトルは日本語**（`feat(scope): 日本語タイトル`形式）。本文見出しも日本語

## Issue Driven Development

- 新機能・仕様変更・API / DB / UI をまたぐ変更は、原則として Issue から開始する
- Issue は要件定義として扱い、背景・目的・スコープ・受け入れ条件・参照ファイルを明記する
- 3ステップ以上の実装や横断変更では、コード変更前に `docs/tasks/{YYYYMMDD}-{slug}.md` に実行計画書・設計を作る
- 実装中に Issue の範囲を超える変更が必要になった場合は、計画書を更新するか別 Issue に分ける
- 詳細: [docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md](docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md)

## モノレポ構成

- `backend/` — Go API（Render）
- `apps/mobile/` — KMP shared + Android composeApp + iosApp
- `docs/tasks/` — issue-body / pr-body / 実行計画書

## ブランチ命名

- `feat/{issue-slug}` または `feat/{numbers}-{slug}-batch`
- `main` / `master` へ直接 push しない
