package com.example.exp4.storage

import android.content.Context
import com.example.exp4.model.Player

/**
 * 全局的玩家数据仓库（单例）。
 *
 * 内存中的 [player] 是各界面共用的同一份数据；
 * 通过 [load] / [save] 把它序列化到应用私有文件 player.dat，
 * 退出程序后重新打开即可恢复个人信息与历史最高分。
 */
object PlayerRepository {

    /** 玩家信息文件名。 */
    private const val FILE_NAME = "player.dat"

    /** 默认玩家信息，首次运行或文件不存在时使用。 */
    val player: Player = Player(
        name = "李飞乐",
        gender = "男",
        studentId = "2024110516",
        phone = "13888888888",
        email = "email@email.com",
        hobbies = "旅游、阅读",
        highScore = 0
    )

    /** 从文件恢复玩家信息；文件不存在时保留默认值。 */
    fun load(context: Context) {
        val saved = ObjectFileStore.load<Player>(context, FILE_NAME) ?: return
        // 拷贝到单例对象中，保证所有界面持有的引用始终有效
        player.name = saved.name
        player.gender = saved.gender
        player.studentId = saved.studentId
        player.phone = saved.phone
        player.email = saved.email
        player.hobbies = saved.hobbies
        player.highScore = saved.highScore
    }

    /** 把当前玩家信息序列化保存到文件。 */
    fun save(context: Context): Boolean {
        return ObjectFileStore.save(context, FILE_NAME, player)
    }

    /**
     * 刷新当前最高分（只在超过历史最高分时更新）。
     *
     * @return 是否刷新了最高分。
     */
    fun updateHighScore(score: Int): Boolean {
        if (score <= player.highScore) return false
        player.highScore = score
        return true
    }
}
