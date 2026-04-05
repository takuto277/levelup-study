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
     * iOS シミュレータからは localhost でアクセス可能
     * TODO: BuildConfig または環境変数から読み込むように変更
     */
    const val BASE_URL = "http://localhost:8080"

    // ── User ────────────────────────────────────
    /** POST: ユーザー作成 */
    const val USERS = "/api/v1/users"

    /** GET/PUT/DELETE: ユーザー取得・更新・削除 */
    fun user(userId: String) = "/api/v1/users/$userId"

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

    // ── User Weapons ────────────────────────────
    /** GET: 所持武器一覧 */
    fun userWeapons(userId: String) = "/api/v1/users/$userId/weapons"

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
}
