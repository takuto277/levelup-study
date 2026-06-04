## Issues

- Close #10

## Why

`StudyService.CompleteStudy` の報酬計算とセッション保存が、設計書の `genre_id` / `dungeon_id`、石報酬ボーナス、ダンジョン進行の流れに追いついていませんでした。特に `dungeonRepo` は DI されているものの未使用で、勉強完了後に `user_dungeon_progress` が更新されない状態でした。

## Summary

実行計画書でスコープと互換性方針を整理したうえで、CompleteStudy を設計書寄りに修正しました。既存モバイルの pending 同期が送る `category` は残しつつ、UUID として扱える場合は `genre_id` にも保存します。

## Changes

- `docs/tasks/20260604-study-complete-design.md` に実行計画と設計を追加
- `StudySession` / `CompleteStudyRequest` に `genre_id` / `dungeon_id` を追加
- 石報酬を 10分ごと +5、30分 / 60分 / 日次2時間ボーナスへ変更
- `GetDailyStudySecondsTx` を使い、日次2時間ボーナスを初回到達時のみ付与
- 日次集計の基準日を UTC に正規化
- `users.selected_dungeon_id` または request の `dungeon_id` を使ってダンジョン進行を更新
- `user_dungeon_progress` の `(user_id, dungeon_id)` を unique 化し、upsert 前提に合わせる
- OpenAPI の study schema と reward_type enum を更新
- 報酬表、日次ボーナス、UTC 日次集計、genre_id 互換、ダンジョン進行の単体テストを追加

## Verification

- `ruby -e 'require "yaml"; YAML.load_file("backend/api/openapi.yaml"); puts "yaml ok"'` - passed
- `backend/` で `GOCACHE=/private/tmp/go-build-cache CGO_ENABLED=1 go test ./internal/service -count=1` - passed
- `backend/` で `GOCACHE=/private/tmp/go-build-cache CGO_ENABLED=1 go test ./... -count=1` - passed
- `backend/` で `GOCACHE=/private/tmp/go-build-cache go vet ./...` - passed
- `GOCACHE=/private/tmp/go-build-cache ./scripts/validate-pr.sh` - passed
