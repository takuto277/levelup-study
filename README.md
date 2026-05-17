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
| [Auto open draft PR](.github/workflows/open-pr-on-push.yml) | `main` 以外への `push` | 同じヘッドのオープン PR が無ければドラフト PR を作成 |
| [Create issue (manual)](.github/workflows/create-issue.yml) | `workflow_dispatch` | Actions タブから手動でイシュー起票 |

イシューは [テンプレート](.github/ISSUE_TEMPLATE/) から作成できます。PR には [テンプレート](.github/pull_request_template.md) が挿入されます。

### ディレクトリ構成
- `.rulesync/`: AI 設定のソースファイル
- `.cursor/rules/`: Cursor 用の自動生成ルール (編集禁止)
- `CLAUDE.md`: Claude Code 用の自動生成ガイド (編集禁止)
- `docs/`: プロジェクトの設計・計画ドキュメント
