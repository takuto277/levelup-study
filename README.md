# LevelUp Study

勉強時間に応じて RPG の冒険が進む学習アプリ。

## AI ルール管理 (rulesync)

このプロジェクトでは [rulesync](https://github.com/dyoshikawa/rulesync) を使用して、Cursor や Claude などの AI アシスタント向け設定を一元管理しています。

### ルールの更新方法

1. `.rulesync/` 配下のファイルを編集します。
   - `rules/`: `always.md`, `kmp.md`, `frontend.md`, `backend.md`, `server.md`
   - `skills/`: AI の特定の動作を定義するスキル
   - `commands/`: AI が実行できるカスタムコマンド
2. 以下のコマンドを実行して、各ツールの設定ファイルを生成・同期します。

```bash
npx rulesync generate
```

> **Note**: グローバルにインストールしている場合は `rulesync generate` でも実行可能です。

## GitHub Actions（CI / PR / イシュー）

| Workflow | トリガー | 内容 |
|----------|----------|------|
| [Backend — CI](.github/workflows/backend-ci.yml) | `push`（`main`）/ `pull_request`（`backend/**` など） | `go test` / `go vet` |
| [Mobile — CI](.github/workflows/mobile-ci.yml) | `push`（`main`）/ `pull_request`（`apps/mobile/**`） | KMP `:shared:compileDebugKotlinAndroid` |
| [iOS — CI](.github/workflows/ios-ci.yml) | `pull_request`（`apps/mobile/iosApp/**`） | SwiftLint + iOS Simulator ビルド |
| [Auto open PR](.github/workflows/open-pr-on-push.yml) | `main` 以外への `push` | オープン PR が無ければ PR 自動作成（日本語テンプレ + ユーザーチェックリスト） |
| [Asset pipeline validation](.github/workflows/assets-ci.yml) | `pull_request`（アセット関連パス） | manifest / sync / slug 検証 |
| [Master images validation](.github/workflows/master-images-ci.yml) | `pull_request`（`backend/assets/master/**` 等） | Supabase マスタ manifest 検証 |
| [Create issue (manual)](.github/workflows/create-issue.yml) | `workflow_dispatch` | Actions タブから手動でイシュー起票 |

イシューは [テンプレート](.github/ISSUE_TEMPLATE/) から作成できます。PR には [テンプレート](.github/pull_request_template.md) が挿入されます。

### GitHub CLI（`gh`）とトークンでイシューを作る

非対話環境（Cursor のエージェントや CI 相当のシェル）では、**[GitHub CLI](https://cli.github.com/)** と **Personal Access Token** を使うと `gh issue create` が使えます。

1. **インストール**（例: macOS）: `brew install gh`
2. **トークン**: GitHub → Settings → Developer settings → **Fine-grained PAT**（当該リポジトリで *Issues: Read and write*）または **Classic PAT**（`repo`）を作成する。
3. **環境変数**: `GH_TOKEN` または `GITHUB_TOKEN` にトークンを入れる（どちらも `gh` が参照する。コミット・シェル履歴に残さないこと）。
4. **実行**:

```bash
export GH_TOKEN=（あなたの PAT）
./scripts/create-issue.sh --title "chore: 例" --body "本文" --label enhancement
```

**zsh / bash の注意:** `--body` を **ダブルクォート `"..."` で渡すと**、本文中の **バッククォート `` ` `` で囲んだ部分がコマンド置換**されます（`RENDER.md` などが「コマンド」として実行されようになる）。Markdown にコードやファイル名を `` ` `` で書くときは、**`--body` 全体をシングルクォート `'...'` で囲む**か、**`--body-file`** に本文を書いて渡してください。

**`GraphQL: Resource not accessible by personal access token (createIssue)` のとき:** PAT に **イシュー作成権限**がありません。**Fine-grained** なら対象リポジトリを選び、**Repository permissions → Issues: Read and write** を付与して再生成する。**Classic** なら **`repo`** スコープ付きで作り直す。組織リポジトリなら **PAT の SSO 承認**が必要な場合があります。

スクリプトは [scripts/create-issue.sh](scripts/create-issue.sh)。リポジトリルートで `git remote` が解決できる前提です。

### 機能開発を一括で始める（Issue + ブランチ）

```bash
export GH_TOKEN=（あなたの PAT）
./scripts/feature-start.sh \
  --title "feat: 機能名" \
  --body-file docs/tasks/issue-body-xxx.md \
  --branch-slug "my-feature"
```

AI 向けの詳細手順: [.cursor/skills/feature-delivery/SKILL.md](.cursor/skills/feature-delivery/SKILL.md)

### ゲームアセット（敵・背景・スプライト）の入稿

正本は `apps/mobile/assets/`。手順は [docs/assets/01_Asset_Ingestion_Workflow.md](docs/assets/01_Asset_Ingestion_Workflow.md)。

```bash
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

### ディレクトリ構成
- `.rulesync/`: AI 設定のソースファイル
- `.cursor/rules/`: Cursor 用の自動生成ルール (編集禁止)
- `CLAUDE.md`: Claude Code 用の自動生成ガイド (編集禁止)
- `docs/`: プロジェクトの設計・計画ドキュメント
- `scripts/`: 開発用スクリプト（`create-issue.sh`, `feature-start.sh`, `assets/`）
