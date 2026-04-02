---
root: true
---
# Always Rules (Core Guidelines)

このファイルは、すべての AI アシスタントが常に従うべき最上位の原則を定義します。

## プロジェクト概要
- **LevelUp Study**: 勉強時間に応じて冒険が進む RPG 型学習アプリ。
- **構成**: モノレポ (`apps/mobile`, `backend`)。

## AI ワークフロー
1. `docs/` を読み、仕様を理解する。
2. 変更計画を立て、必要なら `docs/planning/` に実行計画書を作成する。
3. 最小差分で実装し、ビルド・テストを確認する。
4. 完了後、変更内容を簡潔に報告する。

## 優先順位
1. `always.md` (このファイル)
2. 各カテゴリ別ルール (`frontend.md`, `backend.md` 等)
3. `docs/` 配下の設計ドキュメント
4. 既存のコード実装
