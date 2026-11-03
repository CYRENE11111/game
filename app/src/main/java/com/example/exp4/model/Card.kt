package com.example.exp4.model

import java.io.Serializable

/**
 * 一张扑克牌（游戏模型 Model —— 参照附件代码实现）。
 *
 * 实现 [Serializable]，可以随游戏对象一起写入文件。
 *
 * @param suit  花色：♥ ♦ ♠ ♣
 * @param rank  点数：A 2 3 ... 10 J Q K
 */
class Card(
    private var suit: String,
    private var rank: String,
    var isChosen: Boolean = false,
    var isMatched: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        val rankStrings = arrayOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val validSuits = arrayOf("♥", "♦", "♠", "♣")
    }

    override fun toString(): String {
        return suit + rank
    }

    /** 供界面（View）显示使用。 */
    fun suitString(): String = suit

    /** 供界面（View）显示使用。 */
    fun rankString(): String = rank

    /** 红色花色：红桃 / 方块。 */
    fun isRedSuit(): Boolean = suit == "♥" || suit == "♦"

    /**
     * 配对规则：当传入的牌与当前牌点数都相同时视为配对成功并返回 4 分；
     * 否则返回 0 分。两张牌玩法传入 1 张牌，三张牌玩法传入 2 张牌。
     *
     * @param otherCards 其他已翻开的牌。
     */
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.isEmpty()) return 0
        return if (otherCards.all { it.rank == rank }) 4 else 0
    }
}
