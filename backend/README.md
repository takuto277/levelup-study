# LevelUp Study — Backend (Go)

勉強RPGアプリのバックエンド API サーバー。

## 技術スタック

| 項目 | 技術 |
|---|---|
| 言語 | Go 1.25 |
| ルーター | chi v5 |
| ORM | GORM |
| DB | PostgreSQL 16 |
| 認証 | JWT (Supabase Auth) |
| テスト | SQLite in-memory |

---

## 🚀 ローカル開発セットアップ

### 前提条件

- Go 1.25+
- Docker Desktop（ローカル DB 用）

### 1. 環境変数を設定

```bash
cd backend
cp .env.example .env
```

`.env` を開いて必要な値を埋める（ローカル開発ならデフォルト値のままでOK）。

### 2. ローカル DB を起動

```bash
make db-up
# または: docker compose up -d
```

これで `localhost:5432` に PostgreSQL が起動する。

### 3. サーバーを起動

```bash
make run
# または: go run ./cmd/api
```

`http://localhost:8080` でアクセス可能。初回起動時に GORM がテーブルを自動作成する。

### 4. テスト

```bash
make test
# または: CGO_ENABLED=1 go test ./... -v -count=1
```

テストは SQLite in-memory を使うので PostgreSQL 不要。

---

## 🌐 Supabase を使う場合（本番 / ステージング）

Supabase は PostgreSQL + 認証 + リアルタイム等を提供する BaaS。

### セットアップ手順

1. **[supabase.com](https://supabase.com) でアカウント作成**（GitHub ログイン可）

2. **新しいプロジェクトを作成**
   - Organization: 任意の名前
   - Project name: `levelup-study`
   - Database Password: 安全なパスワードを設定（**メモしておく**）
   - Region: `Northeast Asia (Tokyo)` を選択
   - 「Create new project」をクリック

3. **必要な情報を取得**（プロジェクト作成後）

   **Settings > Database** から：
   ```
   DATABASE_URL = postgresql://postgres.[project-ref]:[password]@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres
   ```
   ※ 「Connection string」の URI 形式をコピー → `[YOUR-PASSWORD]` を手順2のパスワードに置換

   **Settings > API** から：
   ```
   JWT_SECRET = （JWT Secret の値をコピー）
   ```

4. **`.env` を更新**
   ```
   DATABASE_URL=postgresql://postgres.xxxxx:password@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres
   JWT_SECRET=（上でコピーした値）
   API_KEY=（openssl rand -hex 32 で生成）
   ```

5. **サーバー起動**
   ```bash
   make run
   ```
   初回起動時に GORM が Supabase の PostgreSQL にテーブルを自動作成する。

---

## 📁 ディレクトリ構成

```
backend/
├── cmd/api/main.go           # エントリーポイント
├── internal/
│   ├── database/             # DB 接続 & マイグレーション
│   ├── handler/              # HTTP ハンドラー（リクエスト/レスポンス）
│   ├── middleware/            # 認証・CORS・レートリミット等
│   ├── model/                # GORM モデル定義
│   ├── repository/           # DB 操作（CRUD）
│   ├── router/               # ルーティング定義
│   ├── service/              # ビジネスロジック
│   └── testutil/             # テストヘルパー
├── .env.example              # 環境変数テンプレート
├── docker-compose.yml        # ローカル PostgreSQL
├── Makefile                  # 開発コマンド集
└── go.mod
```

---

## 🔒 セキュリティ構成

```
リクエスト
  → CORS チェック
  → グローバル Rate Limit (100 rps)
  → API Key 検証 (X-API-Key ヘッダー)
  → JWT 認証 (Authorization: Bearer)
  → Owner Guard (トークンの sub == URL の {userID})
  → ハンドラー
```

| エンドポイント | API Key | JWT | Owner Guard |
|---|---|---|---|
| `POST /users` | ✅ | ❌ | ❌ |
| `/users/{userID}/*` | ✅ | ✅ | ✅ |
| `/master/*` | ✅ | ❌ | ❌ |
| `/gacha/pull` | ✅ | ✅ | ✅ + Rate Limit 5rps |

---

## 📝 使えるコマンド一覧

```bash
make help       # コマンド一覧を表示
make setup      # 初回セットアップ（.env作成 + DB起動）
make run        # サーバー起動
make test       # テスト実行
make build      # バイナリビルド
make lint       # go vet
make db-up      # DB 起動
make db-down    # DB 停止
make db-reset   # DB リセット（データ削除）
```
