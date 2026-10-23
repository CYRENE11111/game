package com.example.exp4.model

/**
 * 一张扑克牌（游戏模型 Model —— 参照附件代码实现）。
 *
 * @param suit  花色：♥ ♦ ♠ ♣
 * @param rank  点数：A 2 3 ... 10 J Q K
 */
class Card(
    private var suit: String,
    private var rank: String,
    var isChosen: Boolean = false,
    var isMatched: Boolean = false
) {
    companion object {
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
     * 三张牌配对的规则：当传入的两张牌与当前牌点数都相同时（三张同点数），
     * 视为配对成功并返回 4 分；否则返回 0 分。
     *
     * @param otherCards 另外两张已翻开的牌。
     */
    fun match(otherCards: Array<Card>): Int {
        if (otherCards.size < 2) return 0
        return if (otherCards.all { it.rank == rank }) 4 else 0
    }
}
