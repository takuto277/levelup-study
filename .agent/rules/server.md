---
trigger: always_on
---
# Server Rules (Infrastructure)

インフラ、デプロイ、およびサーバー設定に関するルール。

## 採用環境（これ以外は使わない）

| 用途 | 構成 |
|------|------|
| **本番 API** | **Render** — Docker Web Service（`render.yaml` / `backend/Dockerfile`） |
| **ローカル API + DB** | **Docker Compose** — PostgreSQL（`backend/docker-compose.yml`）+ `make run` |
| **DB（本番）** | **Supabase** PostgreSQL |

Vercel / Railway / Fly.io 等の記述や設定ファイルは **追加しない**。ドキュメントに残っていたら削除・Render + Docker に更新する。

## 原則

- コンテナ化（Docker）を基本とする。
- 本番デプロイは Render の GitHub 連携（Actions は `go test` のみ）。
- 手順の正本: `backend/RENDER.md`（本番）、`backend/README.md`（ローカル）。
