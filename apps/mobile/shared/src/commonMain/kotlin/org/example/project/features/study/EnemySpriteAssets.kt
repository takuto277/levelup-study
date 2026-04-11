package org.example.project.features.study

/**
 * 敵の論理キー（ゲームデータ）→ バンドル内に存在するスプライトキーへ寄せる。
 * iOS / Android の drawable・Asset に `sprite_enemy_{key}_1` 形式で置く。
 */
object EnemySpriteAssets {

    private val bundledKeys: Set<String> = buildSet {
        addAll(
            listOf(
                "slime", "goblin", "bat", "skeleton", "dragon", "orc",
                "golem", "lich", "wyvern", "minotaur", "mandrake", "griffin", "basilisk", "cerberus",
                "kraken", "hydra", "leviathan", "banshee", "wisp", "imp", "succubus", "dullahan",
                "gargoyle", "specter", "mummy", "troll", "kobold", "lamia", "crimson_demon", "shadow_knight",
                "harpy", "centaur", "dryad", "naga", "ogre", "cyclops", "ghoul", "wraith", "frost_giant",
                "sand_worm", "cockatrice", "werewolf", "vampire", "zombie", "lizardman", "dark_knight",
                "rust_monster", "bone_dragon", "nightmare", "pirate_wraith", "stone_sentinel", "thunderbird",
                "phoenix", "giant_spider", "ice_witch", "corrupted_paladin", "abyssal_serpent", "treant",
                "chimera", "mimic"
            )
        )
    }

    private val fallbackByLogical: Map<String, String> = emptyMap()

    fun drawableKey(logicalKey: String): String {
        val k = logicalKey.trim().lowercase()
        if (k.isEmpty()) return "slime"
        if (k in bundledKeys) return k
        return fallbackByLogical[k] ?: "slime"
    }
}
