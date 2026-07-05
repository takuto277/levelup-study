# Issue #164: ゲストセッション認証 設計・実行計画

## 1. 目的

ログイン UI を追加せず、各端末を Supabase の匿名ユーザーとして識別する。モバイルアプリは Supabase Anonymous Sign-In で取得したセッションを安全に保存し、JWT の `sub` をユーザー ID として既存 API とデータを利用する。

開発時は既存の Seed ユーザーを残し、Debug ビルドだけ `Seed / Guest` を切り替えられるようにする。Release ビルドは常に Guest を使用する。

将来メール、Apple、Google などのログインを追加するときは、匿名ユーザーへ認証 Identity をリンクし、同じユーザー ID と既存データを引き継げる構造にする。

## 2. 背景と現状

- モバイルには Supabase Auth SDK、ログイン UI、匿名サインイン処理がまだない。
- `UserSessionStore` は `auth_token` を通常の `KeyValueStore` に保存している。
  - Android: `SharedPreferences`
  - iOS: `NSUserDefaults`
  - access token / refresh token の保存先としては不適切なため、平文保存を廃止する。
- `ApiClient` は `UserSessionStore.authToken`、次に `DevJwtSelector.current` を参照する。
- Backend は JWT の `sub` を利用する認証 Middleware と Owner Guard を持つ。
- `POST /api/v1/auth/user` は JWT の `sub` から public `users` を作成・取得する用途だが、現在は Context のキーと型が Middleware と Handler で一致しておらず、非開発 JWT では認証失敗する可能性がある。
- Issue #8 は将来のログイン UI と認証 Identity の追加を扱う。Issue #164 ではログイン UI を実装しない。

## 3. 用語

| 用語 | 意味 |
|------|------|
| Seed Session | 開発用の固定ユーザー ID と開発 JWT を使う既存方式 |
| Guest Session | Supabase Anonymous Sign-In が作成する匿名ユーザーとセッション |
| Auth User ID | Supabase Auth が生成する UUID。JWT の `sub` と public `users.id` に使用 |
| Identity Link | 将来、現在の匿名ユーザーへメール/OAuth Identity を追加する操作 |

「端末 ID」をクライアントで生成して信用する方式は採用しない。ユーザー ID は Supabase Auth が生成し、サーバーは署名済み JWT の `sub` だけを信用する。

## 4. スコープ

### 対象

- Supabase Anonymous Sign-In による Guest Session の作成
- access token / refresh token の安全な端末保存と復元
- access token の期限切れ前更新と refresh token のローテーション保存
- Debug 設定画面での `Seed / Guest` 切り替え
- Release ビルドでの Guest 固定
- API リクエストへの Guest access token 付与
- JWT `sub` に対応する public `users` の作成・取得
- API 環境ごとの Guest Session 分離
- 認証初期化、エラー、再試行状態の UI 反映
- Backend の `/api/v1/auth/user` Context 参照修正とテスト

### 対象外

- メール、Apple、Google などのログイン UI
- 匿名ユーザーへの Identity Link の実装
- 複数端末間のデータ同期・アカウント復旧 UI
- 匿名ユーザーの自動削除ジョブ
- Backend 認証方式全体の刷新

## 5. 確定事項

1. Debug ビルドの初期値は、既存開発フローを壊さないよう `Seed` とする。
2. Debug ビルドでは設定画面から `Seed / Guest` を切り替えられる。
3. Release ビルドでは切り替え UI を表示せず、常に `Guest` とする。
4. Guest Session は API 環境単位で保存する。dev/stg/prod 間でトークンを共有しない。
5. Guest から Seed へ切り替えても Guest Session は削除しない。Guest に戻ったとき同じユーザーへ復帰する。
6. access token と refresh token は安全なストレージへ保存し、通常の設定ストレージへは保存しない。
7. Secure Storage が利用できない場合は平文へフォールバックしない。
8. 既存セッションの refresh token が無効な場合、新しい匿名ユーザーを黙って作らない。データ引き継ぎ不能を明示し、再試行またはゲスト再作成をユーザー操作で選ばせる。
9. 初回起動でセッションが存在しない場合は、匿名ユーザーを自動作成する。
10. 将来のログインは、匿名セッション中に Identity Link を行い、Auth User ID を維持する。

## 6. アーキテクチャ

### 6.1 コンポーネント

| コンポーネント | 配置 | 責務 |
|----------------|------|------|
| `SessionMode` | `commonMain` | `SEED` / `GUEST` の表現 |
| `SessionModeStore` | `commonMain` | Debug の選択モードだけを通常ストレージへ保存 |
| `SecureSessionStore` | `commonMain` + `expect/actual` | 環境別 Guest Session の安全な保存・読込・削除 |
| `GuestAuthService` | `commonMain` | Supabase anonymous sign-in、refresh、現在セッション取得 |
| `SessionManager` | `commonMain` | Build 種別、モード、環境に応じた認証状態機械の管理 |
| `AuthTokenProvider` | `commonMain` | API に付与する現在の access token を一元提供 |
| `UserGateway` | `commonMain` | `POST /api/v1/auth/user` で public user を同期 |

Supabase の認証プロトコルは独自実装せず、KMP 対応の Supabase Kotlin Auth クライアントを version catalog で固定して利用する。

### 6.2 Guest Session モデル

```kotlin
data class StoredGuestSession(
    val environment: String,
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)
```

- 保存キーは `guest_session_{environment}` とし、Supabase project をまたいで復元しない。
- refresh 成功時は新しい access token、refresh token、有効期限を一括で上書きする。
- 書き込み途中のクラッシュで token pair が分離しないよう、1 レコードとして原子的に保存する。
- ログ、Analytics、Crash report に token を出力しない。

### 6.3 プラットフォーム別 Secure Storage

#### Android

- Android Keystore でアプリ専用 AES-GCM キーを生成する。
- セッション JSON を暗号化し、暗号文、IV、フォーマットバージョンだけを専用 `SharedPreferences` または DataStore に保存する。
- バックアップ復元先で Keystore キーと暗号文が不整合になる場合は、復号失敗として扱う。

#### iOS

- Keychain の Generic Password として保存する。
- `service` は Bundle ID を含む固定値、`account` は環境別キーとする。
- アプリ再起動後も復元でき、アンインストール後の残存方針は実装時に Keychain accessibility と合わせてテストする。

### 6.4 モバイル向け Supabase 設定

API 環境ごとに以下を BuildConfig 相当へ注入する。

- Supabase URL
- Supabase publishable key、または移行前プロジェクトの anon key

service role key、JWT secret はモバイルへ含めない。設定が欠けている環境で Guest を選択した場合は、設定不足として明示的に失敗させる。

## 7. セッション状態機械

```kotlin
sealed interface SessionState {
    data object Initializing : SessionState
    data class Ready(val mode: SessionMode, val userId: String) : SessionState
    data class RecoverableError(val reason: SessionError) : SessionState
    data class ResetRequired(val previousUserId: String?) : SessionState
}
```

### 7.1 起動時

1. Build 種別と現在の API 環境を確定する。
2. Release は `GUEST`、Debug は保存済みモードまたは既定の `SEED` を採用する。
3. Seed の場合は既存の seed user ID と `DevJwtSelector` を有効化し、Guest Session には触れない。
4. Guest の場合は対象環境の Secure Storage を読む。
5. セッションがなければ `signInAnonymously` を呼び、返却セッションを保存する。
6. セッションが有効なら access token を採用する。有効期限が近い、または切れていれば refresh して token pair を保存する。
7. access token を付けて `POST /api/v1/auth/user` を呼び、public `users` を冪等に作成・取得する。
8. JWT `sub`、Guest Session の `userId`、public `users.id` が一致したら `Ready` に遷移する。
9. `Ready` になるまで認証必須 API の画面遷移・リクエストを開始しない。

### 7.2 モード切り替え

#### Seed から Guest

1. `SessionState` を `Initializing` にする。
2. `forceDevSeedUserId` と Seed token の選択を解除する。
3. 対象環境の Guest Session を復元または作成する。
4. public user を同期する。
5. ユーザー依存の ViewModel / cache を破棄または再読込する。

#### Guest から Seed

1. Guest の token を active provider から外す。
2. Secure Storage 上の Guest Session は保持する。
3. seed user ID と開発 JWT を有効化する。
4. ユーザー依存の ViewModel / cache を破棄または再読込する。

### 7.3 API 環境切り替え

- 現在環境のセッションを維持したまま、切り替え先環境のキーで別セッションを復元する。
- Guest モードなら切り替え先で復元、refresh、または匿名作成を行う。
- Seed モードなら既存の環境別 Dev JWT 選択を使う。
- 環境切り替え時はユーザー依存 cache を必ず無効化し、別環境のデータ表示を防ぐ。

## 8. トークン期限とエラー処理

- access token は短命である前提とし、有効期限直前に refresh する。
- refresh token はローテーションされる前提で、refresh 成功ごとに token pair を原子的に更新する。
- API が認証期限切れを返した場合は 1 回だけ refresh と再送を行う。同時リクエストの refresh は single-flight 化する。
- ネットワーク不通は Guest User 消失として扱わず、再試行可能エラーにする。
- 初回起動かつオフラインの場合は Guest を作成できないため、接続確認と再試行 UI を表示する。
- 既存 refresh token の失効・復号不能は `ResetRequired` とする。新しい Guest の作成は、既存データへ戻れない可能性を示した確認操作の後だけ実行する。
- Secure Storage への保存が失敗した場合、そのセッションを利用開始せずエラーにする。

## 9. Backend 変更

### 9.1 `/api/v1/auth/user` の修正

現在の Handler は `r.Context().Value("userID").(uuid.UUID)` を参照する一方、認証 Middleware は typed key に JWT `sub` の文字列を保存している。以下へ統一する。

1. `middleware.UserIDFromContext` で `sub` を取得する。
2. `uuid.Parse` で UUID を検証する。
3. public `users` を ID で取得し、存在しなければ作成する。
4. 同時初期化でも重複エラーにならない冪等な upsert / conflict handling にする。
5. レスポンス ID が JWT `sub` と一致することを保証する。

### 9.2 JWT 検証

- Issue #164 では現行の HS256 検証を前提とし、各 Supabase project と Backend の秘密鍵設定を一致させる。
- `sub`、`exp`、署名方式を必須検証する。
- Supabase project が asymmetric signing key / JWKS を使用する場合は、JWKS 対応を別 Issue として先に実施する。
- 将来的には issuer / audience の検証強化を別 Issue で扱う。

## 10. 将来のログイン紐付け

Issue #8 でログイン機能を追加するとき、Guest Session を維持したまま Supabase Auth の Identity Link / user update を実行する。

```text
匿名 Auth User (id=A)
  -> email / Apple / Google Identity をリンク
通常 Auth User (id=A)
```

Auth User ID が変わらないため、`public.users.id = A` と A が所有する学習データの移行は不要となる。リンク完了前に sign out して別ユーザーとして sign in すると ID が変わるため、将来の UI は「新規ログイン」ではなく「現在のゲストを引き継ぐ」導線として実装する。

## 11. Supabase 運用設定

- dev/stg/prod の各 project で Anonymous Sign-Ins を有効化する。
- 匿名作成 API の abuse 対策として rate limit と CAPTCHA 導入可否を確認する。
- 匿名ユーザーは自動削除されないため、未使用ユーザーの保持・削除方針を別 Issue で決める。
- RLS では `auth.uid()` と所有者 ID を比較し、`is_anonymous` claim を理由にデータ分離を弱めない。

## 12. UI 設計

### Debug 設定画面

- 「セッション」項目に `Seed / Guest` の segmented control を置く。
- 切り替え処理中は操作を無効化し、完了後に対象ユーザーのデータを再読込する。
- Guest 選択時は状態と短縮 User ID を開発情報として表示する。token は表示しない。
- エラー時は原因に応じて「再試行」または「ゲストを再作成」を表示する。

### Release

- セッション切り替え UI と Seed 関連情報をコンパイル対象または表示対象から外す。
- 初期化中はスプラッシュ相当の待機状態、失敗時は再試行画面を表示する。

## 13. 移行方針

- 通常 `KeyValueStore` の既存 `auth_token` は初回移行時に削除する。現在は取得フローがないため Secure Storage へ移植しない。
- Seed Session と既存の環境切り替えは維持する。
- `UserSessionStore` が token と user ID の両方を持つ構造を分割し、秘密情報は `SecureSessionStore`、active identity は `SessionManager` を正本とする。
- 既存 Repository / ViewModel から `forceDevSeedUserId` を段階的に `SessionManager` の active user ID 参照へ寄せる。

## 14. テスト計画

### Mobile unit test

- Debug 初期値が Seed、Release が常に Guest になる。
- Guest 初回起動で anonymous sign-in、保存、public user 同期が順に実行される。
- 再起動時に同じ user ID を復元する。
- 期限直前・期限切れで refresh し、回転後 token pair を保存する。
- 複数 API の同時期限切れで refresh が 1 回だけ実行される。
- Seed へ切り替えても Guest Session が削除されない。
- dev/stg/prod の Guest Session が混在しない。
- refresh token 無効時に匿名ユーザーを自動再作成しない。
- Secure Storage 失敗時に平文保存へフォールバックしない。

### Platform test

- Android Keystore の暗号化、復号、改ざん、キー不整合を確認する。
- iOS Keychain の保存、復元、削除、環境分離を確認する。

### Backend test

- 有効な JWT `sub` で public user を新規作成できる。
- 2 回目は同じ user を取得する。
- 不正 UUID、署名不正、期限切れ JWT を拒否する。
- path user ID と `sub` が異なる場合 Owner Guard が拒否する。

### 手動確認

- 新規インストール、再起動、Guest 復元。
- Seed / Guest を往復して各ユーザーのデータが混ざらない。
- dev/stg 切り替え後に別 Guest として動作する。
- access token 更新後も API が継続利用できる。
- オフライン初回起動、オフライン復帰、refresh token 失効。
- Release ビルドに Seed 切り替えが存在しない。

## 15. 実装手順

1. Supabase project ごとの Anonymous Sign-In と JWT signing 設定を確認する。
2. Mobile の環境設定へ Supabase URL と publishable/anon key を追加する。
3. Supabase Kotlin Auth 依存を version catalog と `shared` に追加する。
4. `SecureSessionStore` と Android/iOS actual、platform test を実装する。
5. `GuestAuthService` と fake を実装する。
6. `SessionModeStore`、`SessionManager`、`AuthTokenProvider` と状態機械テストを実装する。
7. `ApiClient` の token 解決と 1 回 refresh/retry を `AuthTokenProvider` に統合する。
8. `/api/v1/auth/user` の Context 不整合と冪等性を修正し、Backend test を追加する。
9. 起動処理で Session 初期化後に public user を同期する。
10. Debug 設定画面へ `Seed / Guest` 切り替えと状態表示を追加する。
11. 環境・モード切り替え時の ViewModel / cache 再生成を実装する。
12. 既存平文 `auth_token` の削除 migration を追加する。
13. 手動確認と `./scripts/validate-pr.sh` を実行する。

## 16. 受け入れ条件

- [ ] Debug ビルドで `Seed / Guest` を切り替えられる。
- [ ] Debug の初期値は Seed である。
- [ ] Release ビルドは Guest 固定で、Seed UI や Seed token を使用しない。
- [ ] Guest 初回利用時に Supabase がランダムな Auth User ID とセッションを発行する。
- [ ] 再起動後も同じ端末・同じ環境で同じ Guest User ID を復元できる。
- [ ] access token / refresh token は Android Keystore または iOS Keychain を利用して保存される。
- [ ] access token の期限切れ前後に refresh され、ローテーション後の token pair が保存される。
- [ ] API は JWT `sub` に対応する public user のデータだけを読み書きする。
- [ ] Seed / Guest および API 環境の切り替えでユーザーデータが混在しない。
- [ ] 無効な既存セッションから別 Guest を黙って作成しない。
- [ ] 将来、同じ Auth User ID にログイン Identity をリンクできる責務分離になっている。
- [ ] Backend と Mobile の自動テスト、および `./scripts/validate-pr.sh` が成功する。

## 17. 参照

- Issue #164
- Issue #8
- [Supabase Anonymous Sign-Ins](https://supabase.com/docs/guides/auth/auth-anonymous)
- [Supabase Kotlin: Anonymous Sign-In](https://supabase.com/docs/reference/kotlin/auth-signinanonymously)
- [Supabase Sessions](https://supabase.com/docs/guides/auth/sessions)
- [Apple Keychain Services](https://developer.apple.com/documentation/security/keychain-services)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)

