package com.example.exp4.model

import java.io.Serializable

/**
 * 玩家实体类：保存玩家的基本信息（参照《翻牌游戏.pdf》中的界面字段）。
 *
 * 实现 [Serializable]，配合 ObjectOutputStream / ObjectInputStream
 * 可以把整个对象写入应用私有文件，退出程序后重新打开即可恢复。
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
) : Serializable {

    override fun toString(): String {
        return "Player(name=$name, gender=$gender, studentId=$studentId, phone=$phone, " +
                "email=$email, hobbies=$hobbies, highScore=$highScore)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
