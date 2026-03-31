package org.example.project.core.network

/**
 * Go バックエンド (Vercel) の API エンドポイント定義
 */
object ApiRoutes {

    /**
     * ベースURL
     * Vercel デプロイ先に合わせて変更する
     * TODO: BuildConfigまたは環境変数から読み込むように変更
     */
    const val BASE_URL = "https://levelup-study.vercel.app"

    // ── User ────────────────────────────────────
    const val USER_ME = "/api/user/me"

    // ── Study ───────────────────────────────────
    const val STUDY_COMPLETE = "/api/study/complete"
    const val STUDY_SESSIONS = "/api/study/sessions"

    // ── Master Data (Characters) ────────────────
    const val MASTER_CHARACTERS = "/api/master/characters"

    // ── User Characters ─────────────────────────
    const val USER_CHARACTERS = "/api/user/characters"
    fun userCharacterLevelUp(id: String) = "/api/user/characters/$id/levelup"

    // ── Master Data (Weapons) ───────────────────
    const val MASTER_WEAPONS = "/api/master/weapons"

    // ── User Weapons ────────────────────────────
    const val USER_WEAPONS = "/api/user/weapons"
    fun userWeaponLevelUp(id: String) = "/api/user/weapons/$id/levelup"
    fun equipWeapon(characterId: String) = "/api/user/characters/$characterId/equip"

    // ── Party ───────────────────────────────────
    const val PARTY = "/api/user/party"
    const val PARTY_SLOT = "/api/user/party/slot"

    // ── Master Data (Dungeons) ──────────────────
    const val DUNGEONS = "/api/master/dungeons"
    fun dungeonStages(dungeonId: String) = "/api/master/dungeons/$dungeonId/stages"

    // ── Dungeon Progress ────────────────────────
    const val DUNGEON_PROGRESS = "/api/user/dungeon-progress"

    // ── Gacha ───────────────────────────────────
    const val GACHA_BANNERS = "/api/gacha/banners"
    const val GACHA_PULL = "/api/gacha/pull"
    const val GACHA_HISTORY = "/api/gacha/history"
}
