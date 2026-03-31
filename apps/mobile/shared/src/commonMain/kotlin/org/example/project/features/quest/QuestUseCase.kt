package org.example.project.features.quest

/**
 * 冒険画面のユースケース
 *
 * TODO: DungeonRepository 実装後にサーバー連携を追加
 * 現在は QuestViewModel がローカルデータを保持しているため未使用
 */
class QuestUseCase {
    /** ローカルのデフォルトダンジョン一覧を返す */
    fun loadDungeons(): List<Dungeon> {
        return QuestViewModel.getDefaultDungeons()
    }
}
