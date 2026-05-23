# Issue: ドキュメント更新（Roadmap・Architecture 陳腐化）

## 背景

`docs/planning/01_Features_and_Roadmap.md` は「Go Ping API が TODO」のまま更新されておらず、実装済みのガチャ・パーティ・勉強タイマーが「考案」に残っている。`docs/planning/02_Backend_Architecture.md` は Vercel Serverless を記載しているが、実際は Render + Docker で運用している。

## 目的

新規参加者が roadmap と architecture ドキュメントだけ読んで、現状の実装度とインフラ構成を把握できるようにする。

## スコープ（本イシュー）

- `docs/planning/01_Features_and_Roadmap.md` — 実装済み / 部分実装 / 未着手を現状に合わせて更新
- `docs/planning/02_Backend_Architecture.md` — Render デプロイ、実 API パス一覧に更新
- `docs/architecture/01_Overview.md` — 完了済みステップの反映、OpenAPI 未作成の明記
- `backend/RENDER.md` — planning / architecture ドキュメントへの相互リンク追加

## 受け入れ条件

- ロードマップに勉強タイマー・ガチャ・パーティ・バックエンド API が実装済みまたは部分実装として記載されている
- バックエンド設計 doc が Render + Supabase + chi の現構成と一致している
- RENDER.md から関連ドキュメントへ辿れる
