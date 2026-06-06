# Issue: ユーザー定義目標・達成ボーナス機能

## 背景

LevelUp Study は「勉強する → 冒険が進む → 報酬を得る」コアループを持っていますが、現状の目標はセッション単位のタイマー時間が中心です。ユーザー自身が「今日ポモドーロを3回クリアする」「今週数学を5時間やる」のような目標を設定し、達成時にボーナス報酬を得る仕組みがあると、短期の継続動機を作りやすくなります。

## 目的

ユーザーが自分で日次/週次の学習目標を設定し、達成したときにサーバー確定のボーナスガチャ石などを受け取れるようにする。

## スコープ

### Backend

- [ ] 目標定義テーブルを追加する
  - 例: `user_goals`
  - 目標種別: `pomodoro_count`, `study_minutes`, `genre_study_minutes`
  - 期間: `daily`, `weekly`
  - 報酬: stones / gold など
- [ ] 目標進捗を勉強完了時に更新する
- [ ] 達成済み目標の重複報酬受け取りを防ぐ
- [ ] 目標 CRUD API を追加する
- [ ] 達成報酬の付与はサーバー側で確定する

### Mobile

- [ ] 目標一覧画面またはホーム内カードを追加する
- [ ] 目標作成 UI を追加する
- [ ] 勉強完了後に達成した目標とボーナスを表示する
- [ ] オフライン時は pending 同期後に確定報酬として反映する

### OpenAPI / Docs

- [ ] OpenAPI に目標 API を追加する
- [ ] 目標種別・報酬式・日次/週次リセット仕様を docs に記載する

## 仕様例

| 目標 | 条件 | 報酬 |
|---|---|---:|
| 今日ポモドーロ3回 | `is_completed = true` の study session が当日3件 | stones +20 |
| 今日60分勉強 | 当日合計 `duration_seconds >= 3600` | stones +15 |
| 今週数学5時間 | genre 指定の週次合計が5時間 | stones +50 |

## 受け入れ条件

- ユーザーが目標を作成・編集・削除できる
- 勉強完了時に目標進捗が更新される
- 達成時にボーナス報酬が1回だけ付与される
- 達成した目標と報酬がモバイル UI で確認できる
- オフライン pending 同期後も報酬の二重付与が起きない

## 参照

- `backend/internal/service/study_service.go`
- `backend/internal/model/models.go`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/study/`
- `docs/database/01_Database_Schema.md`
