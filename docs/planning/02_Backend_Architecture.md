# バックエンド設計指針 (Backend Architecture)

このドキュメントでは、アプリの拡張性（新キャラ/新ダンジョンの追加、データ永続化、チート防止）を考慮したバックエンド構成の設計指針をまとめます。

## アーキテクチャ構成

将来的に以下の **3層構造** でデータを管理・処理します。

```mermaid
graph TD
    Client[KMP Mobile App (iOS/Android)]
    API[Go API (Vercel Serverless)]
    DB[Supabase (PostgreSQL / Auth)]

    Client -- HTTPS/JSON --> API
    API -- SQL/GORM --> DB
    Client -- Auth Context --> DB
```

### 1. インフラ構成 (Tech Stack)
*   **API Server**: **Go言語** (Hobby/Free) on **Vercel**
    - サーバーレス構成によりリクエスト時のみ動作。低コスト（無料枠内）で運用。
    - ゲームロジック（ガチャの確率、報酬計算、ログの検証）を担当。
*   **Database**: **PostgreSQL** on **Supabase** (Free)
    - 拡張性の高いリレーショナルデータベース。
    - 統計情報（週間・月間集計）やアイテムの紐付けが得意。
*   **Auth**: **Supabase Auth**
    - Apple/Googleログインによるユーザー連携を想定。

## データ同期戦略

### 1. 同期対象データ
以下の情報を優先的にサーバーで一元管理（同期）します。
*   **ユーザー通貨（石）**: 不正防止のためDB側で正解を保持。
*   **所持キャラクター**: ガチャの結果など、価値の高いゲーム資産。
*   **勉強・冒険ログ**: 統計データの元となる詳細な履歴。

### 2. 詳細な勉強ログ (Study Logs)
「どの勉強を何時間したのか」を可視化するため、勉強終了のたびに以下のデータをサーバーに送信します。

| 項目 | 型 | 説明 |
| :--- | :--- | :--- |
| `user_id` | UUID | ユーザー識別子 |
| `category` | String | 勉強カテゴリ（例: 英語, 数学）|
| `duration_sec` | BigInt | 実際に勉強した時間（秒） |
| `is_completed` | Boolean | 目標時間を達成したか |
| `stones_earned` | Integer | 獲得した石の数 |
| `timestamp` | Datetime | 開始/終了日時 |

## コンテンツ更新性 (Content Delivery)
新キャラや新ダンジョンをApp Storeの審査なしで追加するため、以下の仕組みを導入します。
*   **Master Data**: ダンジョン情報やキャラクター性能をJSON等のマスタとしてサーバー側で定義し、起動時にアプリが取得。

## 今後の実装予定 (Next Steps)
1.  **Supabase接続**: GoからDB接続し、データをCRUD（生成・読込・更新・削除）できるようにする。
2.  **APIエンドポイント**: 勉強終了時にログを保存する `POST /api/study/log` 等の作成。
3.  **KMP連携**: Android/iOSからHTTPクライアント（Ktor等）を用いて、APIと通信を開始する。

---
*最終更新日: 2026-03-27*
