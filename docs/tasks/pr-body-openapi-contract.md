## Issues

- Close #28

## Why

バックエンド API のルートやスキーマは、これまで router コメント・handler・model 定義に分散していました。#28 では、Go バックエンドと KMP クライアントが参照できる API 契約として `backend/api/` 配下に OpenAPI 定義を追加することが求められています。

## Summary

現行の公開バックエンド API に対する OpenAPI 3.0 定義を追加します。あわせて、誤って tracked されていた `backend/api` のビルド成果物を削除し、設計上の配置先である `backend/api/openapi.yaml` に置き換えます。

## Changes

- `backend/api/openapi.yaml` を追加し、health / user / study / game / gacha / master / debug の現行ルートを記載
- 既存 handler の入力と model の JSON tag をもとに request / response schema を明文化
- 認証が必要なルート向けに API key / JWT bearer の security scheme を定義

## Verification

- `ruby -e 'require "yaml"; YAML.load_file("backend/api/openapi.yaml"); puts "yaml ok"'` - passed
- `backend/` で `GOCACHE=/private/tmp/go-build-cache CGO_ENABLED=1 go test ./... -count=1` - passed
- `backend/` で `GOCACHE=/private/tmp/go-build-cache go vet ./...` - passed
- `./scripts/validate-pr.sh` - docs / API 契約定義のみの変更として、スクリプト上は対象変更なし
