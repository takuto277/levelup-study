# Issue Driven Development

LevelUp Study では、GitHub Issue を単なる TODO ではなく **要件定義の正本** として扱う。実装者は Issue に書かれた背景、スコープ、受け入れ条件を読み、実装前に `docs/tasks/` の実行計画書と設計で具体化してからコード変更へ進む。

## 基本方針

- Issue は「何を作るか」「なぜ作るか」「どこまでを今回やるか」を固定する場所。
- 実行計画書は「どう作るか」「どの順番で作るか」「どう検証するか」を固定する場所。
- PR は「計画に沿って何を実装したか」「何で検証したか」を説明する場所。

## 標準フェーズ

### 1. Issue 作成 / 選定

- 新機能・仕様変更・複数ファイルにまたがる変更は、原則として Issue から開始する。
- 既存 Issue と重複しないか、open / closed の Issue 一覧を確認する。
- Issue 本文には最低限、背景、目的、スコープ、受け入れ条件、参照ファイルを含める。
- Issue 本文は `docs/tasks/issue-body-{slug}.md` にも残す。

### 2. 実行計画書 / 設計

- 実装前に `docs/tasks/{YYYYMMDD}-{slug}.md` を作成する。
- 計画書には次を含める。
  - 対象 Issue
  - 背景と目的
  - 今回のスコープ / スコープ外
  - 既存コード調査結果
  - データモデル / API / UI の設計
  - 実装手順
  - 検証計画
  - リスクと互換性
- 不明点が実装判断に影響する場合は、実装前に Issue またはチャットで確認する。

### 3. 実装

- 計画書に沿って実装する。
- 計画外の大きな変更が必要になった場合は、計画書を更新してから実装する。
- ついでのリファクタや無関係な整形は避ける。

### 4. 検証

- PR 前に `./scripts/validate-pr.sh` を実行する。
- 変更範囲に応じて backend / mobile / assets の追加検証を行う。
- 実行した検証は PR 本文に記載する。

### 5. PR

- PR 本文は `docs/tasks/pr-body-{slug}.md` に作成し、`gh pr create --body-file` で渡す。
- PR 本文には Issue、Why、Summary、Changes、Verification を含める。
- PR 作成直後にセルフレビューを行い、`[must]` があれば修正して push し直す。

## 禁止事項

- Issue なしで大きな機能実装を始めること。
- 実行計画書なしで、3ステップ以上の変更や API / DB / UI 横断変更を始めること。
- Issue の受け入れ条件を満たしていないのに Close すること。
- PR 本文や Issue 本文をシェル引数へ直書きすること。
- GitHub Actions を Issue / PR 作成の代替にすること。
