# Issue #28 OpenAPI 定義追加 実装計画

| 項目 | 内容 |
|------|------|
| Issue | [#28 OpenAPI 定義の追加](https://github.com/takuto277/levelup-study/issues/28) |
| 種別 | backend / documentation / enhancement |
| 作成日 | 2026-06-04 |
| 目的 | Go backend の公開 API 契約を `backend/api/openapi.yaml` に集約し、KMP クライアントと backend の認識差分を減らす |

## 背景

`docs/architecture/01_Overview.md` では `backend/api/` に OpenAPI 定義を置く方針だが、現状は未作成。API 契約は `backend/internal/router/router.go`、各 handler、KMP の `ApiRoutes.kt`、docs に分散している。

このタスクでは実装コードの挙動は変更せず、現行 router と handler の JSON 入出力を OpenAPI 3.0 定義として明文化する。

## ゴール

- `backend/api/openapi.yaml` を追加する。
- `backend/internal/router/router.go` に登録されている全公開 API を OpenAPI に記載する。
- request body、response body、path/query/header、認証要件を components として整理する。
- `docs/planning/02_Backend_Architecture.md` の OpenAPI 未作成記述を更新する。
- 任意対応として OpenAPI lint/validation をローカルまたは CI に追加できる設計にする。

## 非ゴール

- handler のレスポンス形状変更。
- KMP クライアントコード生成の導入。
- API の追加、削除、認可ロジック変更。
- DEV_MODE 専用 debug endpoint の本番公開。

## 参照する正本

- ルート定義: `backend/internal/router/router.go`
- request/response 実装: `backend/internal/handler/*.go`
- domain model: `backend/internal/model/models.go`
- service DTO: `backend/internal/service/study_service.go`, `backend/internal/service/gacha_service.go`
- backend 方針: `docs/planning/02_Backend_Architecture.md`

## OpenAPI 作成方針

- OpenAPI version は `3.0.3` とする。
- ファイルは単一 YAML として `backend/api/openapi.yaml` に置く。
- `servers` は以下を定義する。
  - `https://levelup-study-api.onrender.com`
  - `http://localhost:8080`
- 認証は `components.securitySchemes` で定義する。
  - `ApiKeyAuth`: `apiKey`, `in: header`, `name: X-API-Key`
  - `BearerAuth`: `http`, `scheme: bearer`, `bearerFormat: JWT`
- `/api/v1/*` は原則 `ApiKeyAuth` が必要。
- `/api/v1/users` は API Key のみ。
- `/api/v1/users/{userID}/*` は `ApiKeyAuth` + `BearerAuth` + Owner Guard 前提として記載する。
- `/api/v1/master/*` の GET は API Key のみ。
- `/api/v1/master/genres` POST と `/api/v1/master/genres/{genreID}` DELETE は API Key + BearerAuth として記載する。
- `/api/v1/debug/users/{userID}/currencies` は DEV_MODE 専用であることを description に明記する。

## 対象エンドポイント

| Method | Path | 認証 | 主な schema |
|--------|------|------|-------------|
| GET | `/` | なし | text/plain health |
| POST | `/api/v1/users` | API Key | `CreateUserRequest` -> `User` |
| GET | `/api/v1/users/{userID}` | API Key + JWT | `User` |
| PUT | `/api/v1/users/{userID}` | API Key + JWT | `UpdateUserRequest` -> `User` |
| DELETE | `/api/v1/users/{userID}` | API Key + JWT | `MessageResponse` |
| POST | `/api/v1/debug/users/{userID}/currencies` | API Key + JWT, DEV_MODE | `DebugPatchCurrenciesRequest` -> `User` |
| POST | `/api/v1/users/{userID}/study/complete` | API Key + JWT | `CompleteStudyRequest` -> `CompleteStudyResponse` |
| GET | `/api/v1/users/{userID}/study/sessions` | API Key + JWT | `StudySessionsResponse` |
| GET | `/api/v1/users/{userID}/characters` | API Key + JWT | `UserCharactersResponse` |
| GET | `/api/v1/users/{userID}/characters/{characterID}` | API Key + JWT | `UserCharacter` |
| PUT | `/api/v1/users/{userID}/characters/{characterID}/equip` | API Key + JWT | `EquipWeaponRequest` -> `MessageResponse` |
| POST | `/api/v1/users/{userID}/characters/{characterID}/level-up` | API Key + JWT | `UserCharacter` |
| GET | `/api/v1/users/{userID}/weapons` | API Key + JWT | `UserWeaponsResponse` |
| POST | `/api/v1/users/{userID}/weapons/{weaponID}/level-up` | API Key + JWT | `UserWeapon` |
| GET | `/api/v1/users/{userID}/party` | API Key + JWT | `PartyResponse` |
| PUT | `/api/v1/users/{userID}/party/{slotPosition}` | API Key + JWT | `UpdatePartySlotRequest` -> `UserPartySlot` |
| DELETE | `/api/v1/users/{userID}/party/{slotPosition}` | API Key + JWT | `MessageResponse` |
| GET | `/api/v1/users/{userID}/dungeons` | API Key + JWT | `DungeonProgressResponse` |
| POST | `/api/v1/users/{userID}/gacha/pull` | API Key + JWT | `GachaPullRequest` -> `GachaPullResponse` |
| GET | `/api/v1/users/{userID}/gacha/history` | API Key + JWT | `GachaHistoryResponse` |
| GET | `/api/v1/master/characters` | API Key | `MasterCharactersResponse` |
| GET | `/api/v1/master/weapons` | API Key | `MasterWeaponsResponse` |
| GET | `/api/v1/master/dungeons` | API Key | `MasterDungeonsResponse` |
| GET | `/api/v1/master/dungeons/{dungeonID}` | API Key | `MasterDungeon` |
| GET | `/api/v1/master/gacha/banners` | API Key | `GachaBannersResponse` |
| GET | `/api/v1/master/genres` | API Key | `StudyGenresResponse` |
| POST | `/api/v1/master/genres` | API Key + JWT | `CreateStudyGenreRequest` -> `MasterStudyGenre` |
| DELETE | `/api/v1/master/genres/{genreID}` | API Key + JWT | 204 no content |

## 共通 components

### Parameters

- `UserIDPath`: UUID string, `userID`
- `CharacterIDPath`: UUID string, `characterID`
- `WeaponIDPath`: UUID string, `weaponID`
- `DungeonIDPath`: UUID string, `dungeonID`
- `GenreIDPath`: UUID string, `genreID`
- `SlotPositionPath`: integer, 1-4
- `LimitQuery`: integer, default 20
- `OffsetQuery`: integer, default 0
- `BannerIDQuery`: UUID string, optional

### Error schema

全 handler の `respondError` は `{ "error": string }` を返す。middleware の `http.Error` も JSON 文字列を返しているが、OpenAPI 上は `ErrorResponse` に統一してよい。

```yaml
ErrorResponse:
  type: object
  required: [error]
  properties:
    error:
      type: string
```

主な status は `400`, `401`, `403`, `404`, `429`, `500`。各 operation には最低限 `400`, `401/403`, `500` を入れ、該当するものだけ `404`, `429` を追加する。

## schema 作成メモ

- UUID は `type: string`, `format: uuid`。
- Go の `time.Time` は `type: string`, `format: date-time`。
- `json.RawMessage` の `rate_table`, `drop_table`, `enemy_composition` はまず `type: array` または `type: object` の緩い schema として定義し、実装が安定したら詳細化する。
- `nullable` が必要なもの:
  - `User.selected_dungeon_id`
  - `MasterDungeon.unlock_condition`
  - `MasterCharacter.idle_animation_url`
  - `MasterGachaBanner.pity_threshold`
  - `StudyReward.item_id`
  - `UserCharacter.equipped_weapon_id`
  - request の `user_character_id`, `user_weapon_id`, `weapon_id`
- `additionalProperties` は原則省略し、既存 JSON と strict さのバランスを取る。

## 実装手順

1. `backend/api/` を作成し、`openapi.yaml` を追加する。
2. `info`, `servers`, `tags`, `securitySchemes`, 共通 parameters/responses を先に定義する。
3. `backend/internal/router/router.go` の順番に沿って `paths` を追加する。
4. 各 operation に `operationId` を付ける。
   - 例: `createUser`, `completeStudy`, `listMasterCharacters`
5. handler/service/model の JSON tag に合わせて schemas を追加する。
6. 認証要件を operation ごとに設定する。
7. `docs/planning/02_Backend_Architecture.md` の `OpenAPI 定義は未作成` を、`backend/api/openapi.yaml` 参照へ更新する。
8. optional: OpenAPI validation を追加する。

## optional validation 案

CI 追加は Issue 上「任意」なので、最初の PR では OpenAPI 本体と docs 更新を優先する。validation を入れる場合は次のどちらかを選ぶ。

### 案 A: Redocly CLI

- メリット: OpenAPI lint と品質チェックが強い。
- デメリット: Node 依存を導入する。
- コマンド例:

```bash
npx --yes @redocly/cli@latest lint backend/api/openapi.yaml
```

### 案 B: kin-openapi

- メリット: backend が Go なので技術スタックに馴染む。
- デメリット: 初回実行時に Go module download が必要。
- コマンド例:

```bash
cd backend && go run github.com/getkin/kin-openapi/cmd/validate@latest api/openapi.yaml
```

採用する場合は `scripts/validate-pr.sh` に組み込むより先に、単独コマンドとして PR の Verification に記載するのが安全。

## 受け入れ条件

- `backend/api/openapi.yaml` が存在する。
- `backend/internal/router/router.go` に登録されている全公開 API が `paths` に含まれている。
- 各 endpoint に method、path params、query params、request body、response body、主要 error response、認証要件が記載されている。
- `docs/planning/02_Backend_Architecture.md` が OpenAPI 定義の存在を案内している。
- YAML として構文が壊れていない。

## 推奨検証

```bash
./scripts/validate-pr.sh
```

OpenAPI validation を追加した場合:

```bash
npx --yes @redocly/cli@latest lint backend/api/openapi.yaml
```

または:

```bash
cd backend && go run github.com/getkin/kin-openapi/cmd/validate@latest api/openapi.yaml
```

## 実装時の注意

- `router.go` のコメント一覧と実際の route 登録に差分があるため、実際の route 登録を優先する。
- `docs/planning/02_Backend_Architecture.md` の endpoint 表は一部古い。OpenAPI 作成時に、`level-up`, `study/sessions`, `gacha/history`, `master/genres/{genreID}` を漏らさない。
- `DELETE /api/v1/master/genres/{genreID}` は成功時 204 で body なし。
- `GET /` は `text/plain` で、JSON ではない。
- middleware の認証エラーは `http.Error` 経由だが、契約上は `ErrorResponse` として扱う。
- DEV_MODE 時は API Key/JWT がスキップされることがあるが、OpenAPI は本番運用の認証要件を記載する。

## 後続タスク候補

- OpenAPI から KMP client DTO / API client を生成する検証。
- Swagger UI / Redoc の静的生成。
- OpenAPI lint を CI workflow に追加。
- handler のレスポンス DTO を明示型に寄せ、OpenAPI とコードの対応を保ちやすくする。
