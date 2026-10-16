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
     * 与另一张牌比较：点数相同得 4 分（配对成功），花色相同得 1 分，否则 0 分。
     */
    fun match(otherCards: Array<Card>): Int {
        var score = 0
        if (otherCards.size == 1) {
            val otherCard = otherCards[0]
            if (otherCard.rank == rank) {
                score = 4
            } else if (otherCard.suit == suit) {
                score = 1
            }
        }
        return score
    }
}
