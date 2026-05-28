package org.example.project.core.network

/**
 * Go バックエンドの API エンドポイント定義
 *
 * パスは Go 側の router.go と 1:1 で対応する:
 *   /api/v1/users                              → ユーザー作成
 *   /api/v1/users/{userID}                     → ユーザー取得/更新/削除
 *   /api/v1/users/{userID}/study/complete      → 勉強完了
 *   /api/v1/users/{userID}/characters          → 所持キャラ一覧
 *   /api/v1/master/characters                  → マスタデータ
 *   etc.
 */
object ApiRoutes {

    /**
     * ベースURL
     * - **本番（Render）**: ダッシュボードに表示される `https://....onrender.com`（末尾スラッシュなし）
     * - **ローカル**: `http://127.0.0.1:8080`（`make run` 中のみ）。実機は Mac の LAN IP
     * 本番 API は `DEV_MODE=false` のとき `X-API-Key` 必須。キーは `apps/mobile/local.properties` の `api.key`
     *（または環境変数 `LEVELUP_API_KEY`）を `backend` の `API_KEY` と揃えること（Gradle が `ApiClient` 用ソースを生成）。
     */
    const val BASE_URL = "https://levelup-study-api.onrender.com"

    // ── User ────────────────────────────────────
    /** POST: ユーザー作成 */
    const val USERS = "/api/v1/users"

    /** GET/PUT/DELETE: ユーザー取得・更新・削除 */
    fun user(userId: String) = "/api/v1/users/$userId"

    /** POST: DEV_MODE のみ — 石・ゴールド増減 */
    fun debugPatchCurrencies(userId: String) = "/api/v1/debug/users/$userId/currencies"

    // ── Study ───────────────────────────────────
    /** POST: 勉強セッション完了 & 報酬確定 */
    fun studyComplete(userId: String) = "/api/v1/users/$userId/study/complete"

    /** GET: 勉強セッション履歴一覧 */
    fun studySessions(userId: String) = "/api/v1/users/$userId/study/sessions"

    // ── User Characters ─────────────────────────
    /** GET: 所持キャラ一覧 */
    fun userCharacters(userId: String) = "/api/v1/users/$userId/characters"

    /** GET: 所持キャラ詳細 */
    fun userCharacter(userId: String, characterId: String) =
        "/api/v1/users/$userId/characters/$characterId"

    /** PUT: 武器装備 */
    fun equipWeapon(userId: String, characterId: String) =
        "/api/v1/users/$userId/characters/$characterId/equip"

    /** POST: キャラクターレベルアップ */
    fun levelUpCharacter(userId: String, characterId: String) =
        "/api/v1/users/$userId/characters/$characterId/level-up"

    // ── User Weapons ────────────────────────────
    /** GET: 所持武器一覧 */
    fun userWeapons(userId: String) = "/api/v1/users/$userId/weapons"

    /** POST: 武器レベルアップ */
    fun levelUpWeapon(userId: String, weaponId: String) =
        "/api/v1/users/$userId/weapons/$weaponId/level-up"

    // ── Party ───────────────────────────────────
    /** GET: パーティ取得 */
    fun party(userId: String) = "/api/v1/users/$userId/party"

    /** PUT/DELETE: パーティスロット操作 */
    fun partySlot(userId: String, slot: Int) = "/api/v1/users/$userId/party/$slot"

    // ── Dungeon Progress ────────────────────────
    /** GET: ダンジョン進行状況一覧 */
    fun dungeonProgress(userId: String) = "/api/v1/users/$userId/dungeons"

    // ── Gacha ───────────────────────────────────
    /** POST: ガチャ実行 */
    fun gachaPull(userId: String) = "/api/v1/users/$userId/gacha/pull"

    // ── Master Data（認証不要） ─────────────────
    const val MASTER_CHARACTERS = "/api/v1/master/characters"
    const val MASTER_WEAPONS = "/api/v1/master/weapons"
    const val MASTER_DUNGEONS = "/api/v1/master/dungeons"
    fun masterDungeon(dungeonId: String) = "/api/v1/master/dungeons/$dungeonId"
    const val MASTER_GACHA_BANNERS = "/api/v1/master/gacha/banners"
    const val MASTER_GENRES = "/api/v1/master/genres"

    /** DELETE: ユーザー追加ジャンルの論理削除 */
    fun masterGenre(genreId: String) = "/api/v1/master/genres/$genreId"
}
