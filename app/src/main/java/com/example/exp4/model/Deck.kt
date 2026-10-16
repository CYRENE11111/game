package com.example.exp4.model

import java.util.Random

/**
 * 一副 52 张的扑克牌牌堆（游戏模型 Model —— 参照附件代码实现）。
 */
class Deck {
    private val cards = mutableListOf<Card>()
    private val r = Random()

    init {
        reset()
    }

    /** 重新装满 52 张牌。 */
    fun reset() {
        cards.clear()
        for (suit in Card.validSuits) {
            for (rank in Card.rankStrings) {
                val card = Card(suit = suit, rank = rank)
                cards.add(card)
            }
        }
    }

    /** 随机抽一张牌，牌堆为空时返回 null。 */
    fun drawRandomCard(): Card? {
        var randomCard: Card? = null
        if (cards.size > 0) {
            randomCard = cards.removeAt(r.nextInt(cards.size))
        }
        return randomCard
    }
}
