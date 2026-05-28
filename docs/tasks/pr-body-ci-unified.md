## Why

CI workflow が path ごとに分散しており、docs/skills のみの PR では Checks がほぼ出ない。また旧 iOS CI は `iosApp/**` のみ監視し `shared/**` 変更時に iOS ビルドが走らない穴があった。

## Summary

6 本の workflow を `.github/workflows/ci.yml` 1 本に統合。すべての PR / main push で **CI** workflow が起動し、変更パスに応じて job を実行する。

## Changes

- 新規 `ci.yml`（detect changes → backend / mobile / ios / assets / master-images / skills / summary）
- 旧 `backend-ci.yml` 等 6 ファイル削除
- `workflow_dispatch` で全 job 手動実行可能
- README / AGENTS / validate-pr.sh の参照更新

## Verification

- push 後 GitHub Actions `CI` workflow が PR で起動すること
