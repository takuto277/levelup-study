package org.example.project.core.network

/**
 * デバッグビルドで切り替え可能な API 接続先環境。
 * prod はリリースビルド固定のため選択肢に含めない。
 */
enum class AppEnvironment(val url: String, val displayName: String) {
    DEV("http://localhost:8080", "dev"),
    STG("https://levelup-study-api-stg.onrender.com", "stg");

    companion object {
        const val ANDROID_DEV_URL = "http://10.0.2.2:8080"

        fun resolveFromUrl(url: String): AppEnvironment? {
            if (url == ANDROID_DEV_URL) return DEV
            return entries.firstOrNull { it.url == url }
        }
    }
}

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

    // prod: https://levelup-study-api.onrender.com（リリースビルド固定、デバッグでは選択不可）
    const val PROD_URL = "https://levelup-study-api.onrender.com"

    /** デバッグビルドでは実行時に AppEnvironment で切り替え可能。デフォルトは dev。 */
    var BASE_URL: String = PROD_URL

    // ── Auth ────────────────────────────────────
    /** POST: 認証ユーザー取得／作成 */
    const val AUTH_USER = "/api/v1/auth/user"

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
