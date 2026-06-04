# StudyService CompleteStudy 設計準拠対応 実行計画

## 対象 Issue

- #10 StudyService CompleteStudy を設計書準拠に修正

## 背景

`StudyService.CompleteStudy` は現行実装で勉強セッション保存・報酬保存・通貨加算・キャラ XP 付与まで行っているが、設計書との差分が残っている。

- 石報酬が「2分ごと +1」になっており、設計書の「10分ごと +5 / 30分 +10 / 60分 +25 / 日次2時間 +50」と異なる
- `DungeonProgressRepository` が DI されているが、勉強完了時にステージ進行へ使われていない
- `study_sessions` は設計書上 `genre_id` / `dungeon_id` を持つが、現行 model は `category` のみ
- モバイルの pending 同期は現時点で `category` に genreId 相当の文字列を入れて送っている

## 今回のスコープ

### 実装する

- `CompleteStudyRequest` に `genre_id` / `dungeon_id` を追加する
- 既存モバイル互換のため `category` は残し、UUID として解釈できる場合は `genre_id` にも保存する
- `StudySession` に `GenreID` / `DungeonID` を追加する
- 石報酬を設計書 §5.1 に合わせる
- 日次2時間ボーナスは「このセッションで初めて2時間以上に到達した場合」に1回だけ付与する
- 勉強完了後、進行対象 dungeon がある場合は `DungeonProgressRepository.AdvanceStage` を呼び出す
- 報酬計算とダンジョン進行の単体テストを追加する
- OpenAPI 定義の `study/complete` request / study session schema を更新する

### 今回は実装しない

- `category` カラムの削除
- DB migration による既存データ移行
- ダンジョン `drop_table` を使った gold / item drop の本格計算
- モバイル側 DTO の `genre_id` / `dungeon_id` 送信対応

## 設計

### リクエスト互換

新 API は以下を受け取る。

- `genre_id`: 新クライアント向けのジャンル ID
- `dungeon_id`: 勉強中に進行する dungeon ID
- `category`: 旧クライアント互換。UUID 文字列なら `genre_id` としても扱う

`dungeon_id` が省略された場合は `users.selected_dungeon_id` を使う。どちらも無い場合は、セッション保存と報酬付与だけ行い、ダンジョン進行はスキップする。

### 報酬計算

石報酬:

| 条件 | reward_type | amount |
|---|---|---:|
| 10分ごと | `stones` | 5 |
| 30分連続達成 | `stones_bonus_30` | 10 |
| 60分連続達成 | `stones_bonus_60` | 25 |
| 日次累計2時間到達 | `stones_bonus_daily` | 50 |

日次ボーナスは `GetDailyStudySeconds` で保存前の当日合計を取得し、`before < 7200 && before + duration >= 7200` のときだけ付与する。

XP は既存の「時間 + 討伐 × 難易度」計算を維持する。gold は現時点で設計書が drop_table 参照のみを示し、固定式がないため今回の直接加算は停止する。

### ダンジョン進行

進行ステージ数は最小実装として `duration_seconds / 600` とする。10分以上の勉強で1ステージ進む。0 の場合は進行しない。

既存 progress があれば `current_stage + advance` へ進める。progress がない場合は `current_stage = 1` から開始し、進行後の値で upsert する。

## 検証

- 報酬表に沿った単体テスト
- 日次2時間ボーナスが初回到達時のみ付く単体テスト
- `category` UUID 互換で `genre_id` が保存される単体テスト
- 勉強完了で `user_dungeon_progress` が更新される単体テスト
- `./scripts/validate-pr.sh`
