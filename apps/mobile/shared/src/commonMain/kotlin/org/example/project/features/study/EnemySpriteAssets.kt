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

    private val fallbackByLogical: Map<String, String> = mapOf(
        "abyssal_serpent" to "abyssal_serpent",
        "bat" to "bat",
        "cave_bat" to "bat",
        "cerberus" to "cerberus",
        "corrupted_paladin" to "corrupted_paladin",
        "crimson_demon" to "crimson_demon",
        "crystal_slime" to "slime",
        "dark_knight" to "dark_knight",
        "dark_miner" to "goblin",
        "dragon" to "dragon",
        "fire_slime" to "slime",
        "forest_slime" to "slime",
        "forgotten_tome" to "lich",
        "gargoyle" to "gargoyle",
        "goblin" to "goblin",
        "golem" to "golem",
        "ink_wraith" to "specter",
        "lava_golem" to "golem",
        "lich" to "lich",
        "lizardman" to "lizardman",
        "mimic" to "mimic",
        "nightmare" to "nightmare",
        "phoenix" to "phoenix",
        "rust_monster" to "slime",
        "specter" to "specter",
        "stone_sentinel" to "stone_sentinel",
        "thunderbird" to "thunderbird",
        "treant" to "treant",
        "werewolf" to "werewolf",
        "wisp" to "wisp",
    )

    fun drawableKey(logicalKey: String): String {
        val k = logicalKey.trim().lowercase()
        if (k.isEmpty()) return "slime"
        if (k in bundledKeys) return k
        return fallbackByLogical[k] ?: "slime"
    }
}
