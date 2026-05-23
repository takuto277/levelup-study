package org.example.project.features.quest

import org.example.project.domain.model.StageDrop

typealias StageDropEntry = StageDrop

/** drop_table JSON から UI 用 [DungeonReward] へ変換（rate=1.0 は確定、stones は低 rate をボーナス表示） */
fun stageDropsToReward(drops: List<StageDrop>): DungeonReward {
    var gold = 0
    var exp = 0
    var stones = 0
    var bonusStones = 0
    var bonusRate = 0f

    for (drop in drops) {
        when (drop.itemType.lowercase()) {
            "gold" -> if (drop.rate >= 1.0) gold += drop.amount
            "xp", "exp" -> if (drop.rate >= 1.0) exp += drop.amount
            "stones", "stone", "gacha_stones" -> {
                if (drop.rate >= 1.0) {
                    stones += drop.amount
                } else {
                    bonusStones = maxOf(bonusStones, drop.amount)
                    bonusRate = maxOf(bonusRate, drop.rate.toFloat())
                }
            }
        }
    }

    return DungeonReward(
        gold = gold,
        exp = exp,
        gachaStones = stones,
        bonusItemName = if (bonusStones > 0) "召喚石 +$bonusStones" else null,
        bonusItemDropRate = bonusRate,
    )
}

/** ステージ敵構成の短いサマリー（例: スライム×2、ゴブリン×1） */
fun stageEnemySummary(enemies: List<org.example.project.domain.model.DungeonStageEnemy>): String {
    if (enemies.isEmpty()) return "敵情報なし"
    return enemies
        .sortedBy { it.sortOrder }
        .joinToString("、") { row ->
            val name = row.monster.name.ifBlank { row.monster.emoji.ifBlank { "？" } }
            if (row.count > 1) "$name×${row.count}" else name
        }
}
