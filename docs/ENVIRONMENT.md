# 環境設定リファレンス（LevelUp Study）

どこに何を入れるか／どれが必須かの早見表。
**「次に忘れる」対策用。** 秘密の実値はこのファイルには書かない（各自のローカルにのみ）。

---

## 0. 結論: 最小構成

### stg で動作確認する（最短・今動いてる構成）
必要なのは `apps/mobile/local.properties` の **4項目だけ**:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
api.key=<stg Render の API_KEY>
stg.supabase.url=https://<stg-ref>.supabase.co
stg.supabase.anon.key=<stg の anon public key>
```

＋ Xcode のスキームを **`iosApp-stg`** にする。

これ以外（`dev.*` / `prod.*` / `*.jwt` / `backend/.env`）は **やらなくてよい**。

### dev（ローカル backend）で動かす
- 上記に加えて `local.properties` の `dev.supabase.url` / `dev.supabase.anon.key`
- `backend/.env` を設定し、**Go + Docker をインストール**して `make run`
- Xcode スキームを **`iosApp`**（=dev, `localhost:8080`）

### prod（Release）
- `local.properties` の `prod.supabase.url` / `prod.supabase.anon.key`
- `api.key` は prod の `API_KEY` と一致
- スキーム **`iosApp-prod`**

---

## 1. 置き場は3つ

| # | ファイル/場所 | 何が入るか | 公開? |
|---|---|---|---|
| A | `apps/mobile/local.properties` | アプリにビルド時埋め込み（クライアント） | 秘密ではない（バイナリから抜ける） |
| B | `backend/.env` | ローカル backend（サーバー） | 秘密・gitignore |
| C | Render ダッシュボード（stg / prod サービス） | 各環境のサーバー | 秘密・リポジトリ外 |

---

## 2. A: `apps/mobile/local.properties`

| キー | 何の値 | いつ必要 | 取得元 |
|---|---|---|---|
| `sdk.dir` | Android SDK のパス | Android ビルド時 | Android Studio |
| `api.key` | アプリが送る X-API-Key | **stg / prod は必須**（dev は `DEV_MODE=true` によりスキップ） | 対象サーバーの `API_KEY` と同じ値 |
| `dev.supabase.url` | dev の Project URL | dev を使うとき | Supabase > Settings > API > Project URL |
| `dev.supabase.anon.key` | dev の anon public key | dev を使うとき | Supabase > Settings > API > `anon public` |
| `stg.supabase.url` | stg の Project URL | stg を使うとき | 同上（stg プロジェクト） |
| `stg.supabase.anon.key` | stg の anon public key | stg を使うとき | 同上 |
| `prod.supabase.url` | prod の Project URL | Release ビルド | 同上（prod プロジェクト） |
| `prod.supabase.anon.key` | prod の anon public key | Release ビルド | 同上 |
| `dev.jwt` | dev 用の開発 JWT | 任意（Seed を非 DEV_MODE サーバーで使う時だけ） | `backend` の `make mint-dev-jwt` |
| `stg.dev.jwt` | stg 用の開発 JWT | 任意（同上） | stg の `JWT_SECRET` で署名 |

> 値を変更したら **リビルド必須**（`GeneratedApiKey.kt` を再生成）。
> 環境変数 `LEVELUP_*` があると `local.properties` より優先される点に注意。

---

## 3. B: `backend/.env`（ローカル backend 用）

| キー | 何の値 | 必須? |
|---|---|---|
| `DATABASE_URL` | ローカル API が接続する DB（Docker or Supabase URI） | 必須 |
| `JWT_SECRET` | Supabase の JWT Secret | 必須 |
| `API_KEY` | サーバーが要求する X-API-Key（Aの `api.key` と一致） | 必須 |
| `SUPABASE_URL` | JWKS 取得用 Project URL（`https://<ref>.supabase.co`） | 必須（Guest の JWT 検証） |
| `DEV_MODE` | `true` で JWT / API Key 検証をスキップ | ローカルは `true` |
| `ENV` | `dev` / `staging` / `production` | 任意 |
| `PORT` | 待受ポート（既定 8080） | 任意 |
| `DEBUG_API_LOG` | 🌱 ログ出力 | 任意 |
| `SUPABASE_SERVICE_ROLE_KEY` | secret key | 画像アップロード時のみ |
| `SUPABASE_STORAGE_BUCKET` | 画像バケット | 画像アップロード時のみ |
| `STG_DATABASE_URL` | `make seed-stg` 用 | stg へシードする時のみ |
| `PROD_DATABASE_URL` | `make seed-prod` 用 | prod へシードする時のみ（危険） |

---

## 4. C: Render ダッシュボード（stg / prod サービス）

各サービス（`levelup-study-api-stg` / `levelup-study-api`）に設定。
`render.yaml` では `sync: false`（＝手動設定）。

| キー | stg | prod |
|---|---|---|
| `DATABASE_URL` | stg 用 Supabase の postgres URI | prod 用 |
| `JWT_SECRET` | stg の JWT Secret | prod |
| `API_KEY` | stg の API Key | prod |
| `SUPABASE_URL` | stg の Project URL | prod |
| `DEV_MODE` | `false` | `false` |
| `DEBUG_API_LOG` | `true` | `false` |
| `ENV` | `staging` | `production` |

---

## 5. 「キー」の種類の早見表（混同しやすい）

| 呼び名 | 実体 | 入る場所 | 役割 |
|---|---|---|---|
| **X-API-Key** | サーバー発行の共有キー | `local.properties` の `api.key` ⇔ サーバーの `API_KEY` | アプリ識別（低い強度。JWTが本体） |
| **Supabase Project URL** | `https://<ref>.supabase.co` | `*.supabase.url`（A）/ `SUPABASE_URL`（B,C） | Auth / JWKS の基点 |
| **anon public key** | 公開前提のキー | A の `*.supabase.anon.key` | モバイルの匿名サインイン |
| **service_role key** | 全権の秘密キー | B の `SUPABASE_SERVICE_ROLE_KEY` | サーバー専用（画像入稿）。**モバイル厳禁** |
| **postgres URI** | `postgresql://...pooler...:6543/...` | B の `DATABASE_URL` / `STG・PROD_DATABASE_URL` | サーバーのDB接続。**モバイル厳禁** |
| **JWT Secret** | Supabase の署名秘密 | B,C の `JWT_SECRET` | HS256 の検証用（Guest は JWKS で検証） |

**覚え方**: `https://<ref>.supabase.co` はモバイル、`postgresql://...` と service_role はサーバーだけ。

---

## 6. 環境とスキーム / URL の対応

| 環境 | Xcode Scheme | `LEVELUP_ENV` | API URL | Supabase |
|---|---|---|---|---|
| dev | `iosApp` | `dev` | `http://localhost:8080` | `dev.supabase.*` |
| stg | `iosApp-stg` | `stg` | `https://levelup-study-api-stg.onrender.com` | `stg.supabase.*` |
| prod | `iosApp-prod`（Release） | （無し→prod固定） | `https://levelup-study-api.onrender.com` | `prod.supabase.*` |

---

## 7. ハマりどころ

- **スキームと `*.supabase.*` が不一致** → Supabase URL 空エラー（「セッションを準備中」のまま／初期化失敗）
- **`api.key` が空 or 不一致** → stg/prod で **403**（dev は `DEV_MODE=true` で通る）
- **`SUPABASE_URL`（サーバー）とアプリの Supabase プロジェクトが不一致** → **401**（JWT 検証失敗）
- **アプリと別プロジェクトの anon key** → Guest ユーザーが別DBに作られる/404
- **`api.key` は単一値**（環境別ではない）→ stg 用に設定すると prod でも同じ値が使われる。将来 `dev/stg/prod.api.key` に分離したい
- 秘密を**一度コミットすると履歴に残る** → 漏れたら値のローテーション必須
