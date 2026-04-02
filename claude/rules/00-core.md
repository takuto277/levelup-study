# LevelUp Study - AI グローバルガイド（共通）

このファイルは、AIアシスタントがこのリポジトリで作業する際の**最上位ガイドライン**です。

---

## 1. リポジトリのコンテキスト
- **プロジェクト名:** LevelUp Study（勉強RPGアプリ）
- **タイプ:** モノレポ（Monorepo）
- **目的:** iOS / Android 向けのゲーム化学習アプリ。
  - 勉強時間 → 冒険進行
  - 勉強報酬 → ガチャ / 育成

## 2. 技術スタック
- **モバイル:** `apps/mobile/`
  - iOS: Swift / SwiftUI
  - Android: Kotlin / Jetpack Compose
  - 共有ロジック: Kotlin Multiplatform (KMP)
- **バックエンド:** `backend/` (Go)

---

## 3. 優先して読むドキュメント（AI必須）
実装前に次の順で確認すること。

1. `docs/architecture/01_Overview.md`
2. `docs/features/*.md`（該当機能）
3. `docs/planning/01_Features_and_Roadmap.md`
4. `claude/rules/*.md`（生成物。編集は `ai/rules/`）

---

## 4. ルールファイルの優先順位
競合した場合は次の優先順位で従うこと。

1. 本ファイル（生成物: `claude.md` / Cursor: `.cursor/rules/00-core.mdc`）
2. `claude/rules/` 配下の個別ルール（生成物）
3. `docs/` の設計書
4. 既存実装

---

## 5. AI実装ワークフロー（必須）
1. 対象機能の `docs` を読む
2. 変更対象を列挙（UI / ViewModel / 必要なら Domain）
3. **最小差分**で実装
4. ビルド・エラー確認
5. 変更点と次アクションを簡潔に報告

---

## 6. アーキテクチャ方針（要点）
1. **UIはネイティブ**（SwiftUI / Compose）
2. **状態とビジネスロジックはKMP**（ViewModel中心）
3. 画面遷移や OS 固有 API はネイティブ側
4. 画面終了可否（例: dismiss）は、可能な限り KMP 状態で判定

---

## 7. 命名・構成ポリシー（要点）
- iOS/Android/KMPで、画面名・状態名・イベント名を揃える
- 例: `HomeScreen`, `HomeUiState`, `HomeIntent`
- 新規実装時は、将来 `UseCase` / `Repository` を追加しやすい階層を維持する

---

## 8. 補足
- バックエンドの Go 実装は Idiomatic Go に従う
- 詳細ルールは個別ルール（生成物: `claude/rules/*.md` / Cursor: `.cursor/rules/*.mdc`）を参照
