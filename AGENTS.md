# AGENTS.md — LevelUp Study エージェント向けリファレンス

AI コーディングエージェント（Cursor 等）が **PR 前の品質確認** と **GitHub 操作** で参照する設定ファイル。
GitHub Actions は push 後の **セーフティネット**。エージェントは **マージ前にローカルで lint / test を実行** する。

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
| iOS (Swift) | `cd apps/mobile && xcodebuild ...` | **ローカル macOS のみ**。CI は macos-15 + Xcode 26.3 |
| Assets | `./scripts/assets/run.sh scripts/assets/validate_assets.py` | assets 変更時 |
| Master images | `cd backend && make master-images-validate` | master 画像変更時 |

## スキル（`.cursor/skills/`）

| スキル | 用途 |
|--------|------|
| `feature-delivery` | LevelUp 標準フロー（Issue → 実装 → validate → PR） |
| `github-implement-pr` | Issue 実装〜PR までの入口（汎用手順） |
| `github-pr-create` | **PR 本文テンプレ + `gh pr create --body-file`** |
| `qa-test-design` | 手動 UI 確認含むテスト観点の洗い出し |
| `git-branch-create` / `git-commit-message` | ブランチ名・コミットメッセージ |

PR 本文の構造は **`.github/pull_request_template.md` ではなく `github-pr-create` スキル** を正とする。
Auto open PR（push 時 Actions）が起動した場合も、エージェントは validate 結果を `Verification` に追記する。

## GitHub CLI

- PR 作成・更新は **`gh` / `gh api`** を使う（GitHub コネクタは 403 になりやすい）
- PR 本文は **`--body-file`** または `gh api --field body=@file`（シェル直埋め込み禁止）

## モノレポ構成

- `backend/` — Go API（Render）
- `apps/mobile/` — KMP shared + Android composeApp + iosApp
- `docs/tasks/` — issue-body / 実行計画書

## ブランチ命名（本リポジトリ慣習）

- `feat/{issue-slug}` または `feat/{numbers}-{slug}-batch`
- `main` / `master` へ直接 push しない
