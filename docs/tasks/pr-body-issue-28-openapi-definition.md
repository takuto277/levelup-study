## Issues

- Refs #28

## Why

`backend/internal/router/router.go` に API ルートは定義されていますが、KMP クライアントと Go backend が共有できる OpenAPI 契約がありませんでした。API 仕様を `backend/api/openapi.yaml` に集約し、実装・ドキュメント・クライアント開発の認識差分を減らします。

## Summary

現行 router / handler / model に沿って OpenAPI 3.0.3 定義を詳細化しました。既に追加された OpenAPI 初版に対して、operationId、server、認証エラー、wrapper response schema などを補強しています。

## Changes

- `backend/api/openapi.yaml` に全公開 API の operationId / path / request / response / auth schema を整理
- production / local development の `servers` を追加
- response wrapper schema と主要 error response を補強
- OpenAPI 実装計画書 `docs/tasks/20260604-openapi-definition-plan.md` を追加
- `docs/planning/02_Backend_Architecture.md` の OpenAPI 未作成表記と endpoint 表を更新
- `docs/architecture/01_Overview.md` と `docs/planning/01_Features_and_Roadmap.md` の OpenAPI 記述を更新

## Verification

- [x] `ruby -e 'require "yaml"; ...'` - OpenAPI YAML parse / `$ref` check passed
- [x] `git diff --check` - passed
- [x] `cd backend && CGO_ENABLED=1 go test ./... -count=1` - passed
- [x] `cd backend && go vet ./...` - passed
- [x] `./scripts/validate-pr.sh` - passed

## Notes

- OpenAPI validation toolingの CI 組み込みは Issue 上でも任意のため、この PR では定義追加と docs 更新を優先しています。
