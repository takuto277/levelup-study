package org.example.project.features.home

object HomeCharacterDialogue {
    val idleMessages: List<String> = listOf(
        "今日の特訓も頑張ろうな！",
        "知識こそ最強の武器だ。",
        "お前の成長、楽しみにしてるぞ。",
        "さぁ、冒険の時間だ！",
        "集中すれば、何でもできる。"
    )

    fun messageAt(index: Int): String {
        if (idleMessages.isEmpty()) return ""
        val normalized = ((index % idleMessages.size) + idleMessages.size) % idleMessages.size
        return idleMessages[normalized]
    }
}
