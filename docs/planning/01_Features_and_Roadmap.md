# LevelUp Study - 機能・ロードマップ (Features & Roadmap)

このドキュメントは、アプリに必要な機能の洗い出しと、それぞれのステータスを管理するためのリストです。
実装を進める前に、まずは「💡 考案」フェーズでアイデアを出し合い、要件定義と設計が固まったものを「🏗 設計中」→「📝 TODO」へと移していきます。

**最終更新:** 2026-06-06

---

## 💡 1. 考案 (Idea / Backlog)
まだアイデア段階、または将来対応予定の機能です。

- **放置冒険（アイドル RPG）**
  - 勉強していない時間にパーティが自動でモンスターを倒し、報酬を持ち帰る
- **衣装（costume）システム**
  - ガチャ石の追加消費先。着せ替え・衣装ガチャ
- **フレンド・ソーシャル機能**
  - 他ユーザーのパーティと対戦、勉強時間の競争

---

## 🏗 2. 設計中 (In Design)
仕様の深掘りや画面設計・API 設計を行っている機能。

- [ ] ユーザー定義目標・達成ボーナス（#49）

---

## 📝 3. TODO (Ready for Implementation)
要件と設計が固まり、実装待ちのタスク。

### 基盤・セキュリティ
- [ ] Supabase Auth ログイン UI とユーザー ID 紐付け（#8）
- [ ] API 認可強化 — リソース所有者検証（#9）

### コアゲームループ
- [ ] StudyService CompleteStudy を設計書準拠に（報酬式・ダンジョン進行）（#10）
- [ ] ガチャ重複処理（凸・精錬）（#11）
- [ ] キャラ・武器 LvUP API + 編成 UI（#12）
- [ ] パーティ武器装備 UI の API 接続（#13）
- [ ] サーバー連動バトル — ダンジョンマスタ利用（#16）
- [ ] バックグラウンドタイマー（#17）

### UX・品質
- [ ] iOS 4 スロットパーティ編成 — Android parity（#14）
- [ ] バトルアセット sync と manifest 整合（#15）
- [ ] 記録タブ — カレンダーヒートマップ + Analytics 整理（#18）
- [ ] 冒険タブ — 報酬表示の API 連動（#19）
- [ ] ホーム画面 — キャラアニメーション・タップ反応（#24）
- [ ] Supabase マスタ画像 CDN パイプライン（#25）
- [ ] 初回オンボーディングとコアループ説明導線（#50）
- [ ] 未同期セッションの状態表示・手動リトライ（#51）

### バックエンド・インフラ
- [ ] ガチャ履歴 API（#20）
- [ ] DB スキーマ差分解消・マイグレーション方針確定（#21）
- [ ] バックエンドテスト拡充（#22）
- [x] OpenAPI 定義（#28）

---

## 🔶 4. 部分実装 (Partial)
骨格は動いているが、設計書との差分や UI 未接続が残る機能。

| 機能 | できていること | 主な残タスク |
|------|---------------|-------------|
| **勉強タイマー + バトル** | StudyQuest 画面、サーバーへの勉強完了同期、オフラインキュー | バックグラウンド計測、サーバー連動バトル、未同期状態の見える化 |
| **ガチャ** | バナー取得・Pull API、天井・featured | 重複凸/精錬、履歴 API、衣装 |
| **パーティ編成** | 4 スロット（Android）、API 連携 | iOS 4 スロット、装備 UI、LvUP |
| **冒険（Quest）タブ** | ダンジョン選択、進行表示 | 報酬ハードコード解消、バトルログ |
| **記録タブ** | 棒グラフ・ジャンル内訳・ストリーク | ヒートマップ、Analytics 死コード整理 |
| **ユーザー認証** | JWT + API Key、dev seed ユーザー | ログイン UI、Auth uid と users.id の一致 |
| **RPG・冒険** | 勉強中クライアント側バトル SIM | サーバーマスタ連動、放置プレイ |

---

## ✅ 5. 実装済み (Done)

### プロジェクト基盤
- [x] モノレポ構成（KMP `apps/mobile/shared` + Go `backend`）
- [x] プロジェクト基本アーキテクチャの策定（`docs/architecture/01_Overview.md`）
- [x] ドキュメント・ルールのテンプレート

### バックエンド（Go on Render）
- [x] chi ルーティング + 20+ REST API（ユーザー / 勉強 / ガチャ / パーティ / マスタ）
- [x] PostgreSQL（Supabase）+ GORM AutoMigrate + seed
- [x] JWT + API Key 認証、Owner Guard
- [x] Render デプロイ（`render.yaml`, Docker）— 本番 URL: `https://levelup-study-api.onrender.com`
- [x] GitHub Actions backend-ci
- [x] StudyService CompleteStudy の設計書準拠（報酬式・日次ボーナス・ダンジョン進行）（#10）

### モバイル（KMP + iOS / Android）
- [x] 5 タブ UI（冒険 / 編成 / ホーム / 召喚 / 記録）
- [x] 勉強モーダル（タイマー + バトル SIM）
- [x] Real API 連携（Repository 層、オフライン pending 同期）
- [x] 設定画面（DEBUG 通貨パッチ等）

### CI
- [x] backend-ci / mobile-ci / ios-ci / assets-ci

---

## 参照

- 詳細設計: `docs/features/`
- バックエンド API 一覧: `docs/planning/02_Backend_Architecture.md`
- GitHub Issues: https://github.com/takuto277/levelup-study/issues
