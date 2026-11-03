package com.example.exp4.storage

import android.content.Context
import com.example.exp4.model.CardMatchingGame
import java.io.Serializable

/**
 * 写入文件的一局游戏快照：游戏对象 + 保存时间 + 玩法（2 张 / 3 张）。
 *
 * 实现 [Serializable]，由 [GameStore] 通过对象流写入 / 读出文件。
 */
class SavedGame(
    val groupSize: Int,
    val savedAt: Long,
    val game: CardMatchingGame
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 游戏进度的文件存储。
 *
 * 两种玩法各自使用一个文件（两牌游戏 game_two_card.dat、三牌游戏 game_three_card.dat），
 * 每次翻牌、重置、退出前都会写入，保证退出程序后重新打开可以恢复。
 */
object GameStore {

    fun save(context: Context, fileName: String, game: CardMatchingGame): Boolean {
        val snapshot = SavedGame(
            groupSize = game.groupSize,
            savedAt = System.currentTimeMillis(),
            game = game
        )
        return ObjectFileStore.save(context, fileName, snapshot)
    }

    /** 读取指定玩法的游戏进度；文件不存在或玩法不匹配时返回 null。 */
    fun load(context: Context, fileName: String, expectedGroupSize: Int): SavedGame? {
        val saved = ObjectFileStore.load<SavedGame>(context, fileName) ?: return null
        if (saved.groupSize != expectedGroupSize || saved.game.groupSize != expectedGroupSize) {
            return null
        }
        return saved
    }
}
