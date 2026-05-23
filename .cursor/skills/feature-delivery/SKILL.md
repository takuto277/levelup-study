---
name: feature-delivery
description: >-
  LevelUp Study の標準機能開発フロー（Issue 起票 → ブランチ → 実行計画書 → 実装 → validate → PR）。
  ユーザーが「実装して」「PR まで出して」と依頼したとき、または 3 ステップ以上の変更タスクで使用する。
---
# Feature Delivery（Issue → PR まで）

ユーザーが毎回口頭でフローを指示しなくてよいよう、以下を **この順序で** 実行する。

## 1. コンテキスト把握

1. `project-context` スキルでプロジェクト概要を確認
2. 関連する `docs/features/` / `docs/tasks/` / `docs/assets/` を読む
3. 3 ステップ以上の変更なら `docs/tasks/YYYYMMDD-<slug>.md` 実行計画書を作成（`task-init` テンプレート）

## 2. Issue 起票

**パターン A — 一括（推奨）**

```bash
export GH_TOKEN=（PAT）
./scripts/feature-start.sh \
  --title "feat: 機能名" \
  --body-file docs/tasks/issue-body-{slug}.md \
  --branch-slug "{slug}"
```

**パターン B — Issue のみ**

```bash
./scripts/create-issue.sh --title "feat: 機能名" --body-file docs/tasks/issue-body-xxx.md
```

**パターン C — push 時自動**  
`feat/{slug}` ブランチに `docs/tasks/issue-body-{slug}.md` を置いて push すると Actions が Issue を起票。

- zsh: `--body` 内のバッククォート注意 → **`--body-file` 推奨**
- アセット追加: `.github/ISSUE_TEMPLATE/asset-ingestion.yml`

## 3. ブランチ

- 命名: `feat/{issue番号}-{slug}` または `feat/{slug}`
- `main` から分岐

## 4. 実装

- 最小差分、`.cursor/rules/` に従う
- **アセット変更時**（必須）:
  ```bash
  ./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py
  ./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
  ./scripts/assets/run.sh scripts/assets/validate_assets.py
  ```
  詳細: `docs/assets/01_Asset_Ingestion_Workflow.md` / `assets` ルール

## 5. 検証

| 変更 | コマンド |
|------|----------|
| アセット | `./scripts/assets/run.sh scripts/assets/validate_assets.py` |
| KMP | `cd apps/mobile && ./gradlew :shared:compileDebugKotlinAndroid` |
| Backend | `cd backend && make test` |

## 6. PR

```bash
git push -u origin HEAD
```

- push で **Auto open draft PR** がドラフト PR を作成
- **失敗時**: GitHub → Settings → Actions → **Allow GitHub Actions to create and approve pull requests** を ON
- ローカル代替: `./scripts/create-pr.sh --title "..." --body "Fixes #n"`
- PR 本文: Summary / Test plan / `Fixes #番号`
- ユーザーがレビュー。勝手にマージしない

## 7. 完了報告

Issue URL・PR URL・変更概要を日本語で簡潔に報告。実行計画書を「完了」に更新。

## 禁止事項

- ユーザー明示なしの commit / force push / マージ
- `assets/source/` を経由せず drawable / xcassets を直接編集
- シークレットのコミット
