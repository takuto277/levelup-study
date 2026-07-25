Please also reference the following rules as needed. The list below is provided in TOON format, and `@` stands for the project root directory.

rules[4]{path}:
  @.codex/memories/backend.md
  @.codex/memories/frontend.md
  @.codex/memories/kmp.md
  @.codex/memories/server.md

# Always Rules (Core Guidelines)

このファイルは、すべての AI アシスタントが常に従うべき最上位の原則を定義します。

## プロジェクト概要
- **LevelUp Study**: 勉強時間に応じて冒険が進む RPG 型学習アプリ。
- **構成**: モノレポ (`apps/mobile`, `backend`)。

## AI ワークフロー
1. `docs/` を読み、仕様を理解する。
2. **Issue Driven Development**: 新機能・仕様変更・API / DB / UI をまたぐ変更は、Issue を要件定義の正本として扱い、[docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md](../../docs/ai/ISSUE_DRIVEN_DEVELOPMENT.md) に従う。
3. **実行計画書の作成**: 3ステップ以上の変更を伴う場合、コード変更前に `docs/tasks/{YYYYMMDD}-{slug}.md` へ実行計画書・設計を作成する。
   - 実装詳細セクションには、具体的なコードの変更内容と、その実装を選択する「理由」を詳しく記述すること。
4. **機能開発（Issue → PR）**: ユーザーが実装〜PR まで依頼した場合、`.cursor/skills/feature-delivery` に従う。Issue / PR / レビューは **`gh` CLI + スキル**（[AGENTS.md](../../AGENTS.md)）。GitHub Actions は lint/test のみ。
5. **アセット入稿**: 画像追加・変更時は `assets` ルールと `docs/assets/01_Asset_Ingestion_Workflow.md` に従う。
6. 最小差分で実装し、ビルド・テストを確認する。
7. 完了後、変更内容を簡潔に報告する。

## 優先順位
1. `always.md` (このファイル)
2. 各カテゴリ別ルール (`frontend.md`, `backend.md` 等)
3. `docs/` 配下の設計ドキュメント
4. 既存のコード実装
