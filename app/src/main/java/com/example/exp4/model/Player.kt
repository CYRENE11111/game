package com.example.exp4.model

/**
 * 玩家实体类：保存玩家的基本信息（参照《翻牌游戏.pdf》中的界面字段）。
 *
 * 说明：按实验要求，修改后的信息只在内存中传递，不写入文件。
 */
class Player(
    var name: String = "",
    var gender: String = "男",
    var studentId: String = "",
    var phone: String = "",
    var email: String = "",
    /** 爱好，多个爱好用“、”分隔。 */
    var hobbies: String = "",
    /** 游戏最高分。 */
    var highScore: Int = 0
) {
    override fun toString(): String {
        return "Player(name=$name, gender=$gender, studentId=$studentId, phone=$phone, " +
                "email=$email, hobbies=$hobbies, highScore=$highScore)"
    }
}

/**
 * 全局的玩家数据仓库（单例，仅存在于内存中）。
 * 主 Activity、信息修改 Activity、翻牌游戏 Activity 都通过它读写同一份玩家信息。
 */
object PlayerRepository {

    /** 默认玩家信息，可在“信息修改”界面中修改。 */
    val player: Player = Player(
        name = "李飞乐",
        gender = "男",
        studentId = "2024110516",
        phone = "13888888888",
        email = "email@email.com",
        hobbies = "旅游、阅读",
        highScore = 0
    )

    /** 刷新当前最高分（只在超过历史最高分时更新）。 */
    fun updateHighScore(score: Int) {
        if (score > player.highScore) {
            player.highScore = score
        }
    }
}
