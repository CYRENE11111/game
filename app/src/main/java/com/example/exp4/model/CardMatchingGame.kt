package com.example.exp4.model

import java.util.Random

/**
 * 翻牌配对游戏（游戏模型 Model —— 参照附件代码实现）。
 *
 * 计分规则（与附件一致）：配对成功 +4 * 4，配对失败 -2，每翻开一张牌 -1。
 *
 * 发牌方式：先随机抽出若干张牌，再复制成“对”，最后洗牌。
 * 这样保证每张牌都一定有自己的配对牌，一局游戏必定可以完成。
 *
 * @param count 一局游戏中牌的数量（应为偶数）。
 */
class CardMatchingGame(count: Int) {

    var score = 0
        private set

    /** 本局一共用了几张牌。 */
    val cardCount: Int = if (count % 2 == 0) count else count + 1

    private val cards: MutableList<Card> = mutableListOf()

    private val random = Random()

    val MISMATCH_PENALTY = 2
    val MATCH_BONUS = 4
    val COST_TO_CHOOSE = 1

    init {
        deal()
    }

    /** 使用附件中的牌堆随机抽牌，再复制成对后洗牌。 */
    private fun deal() {
        val deck = Deck()
        val pairCount = cardCount / 2
        val picked = mutableListOf<Card>()
        for (i in 1..pairCount) {
            val card: Card? = deck.drawRandomCard()
            if (card != null) {
                picked.add(card)
            }
        }
        val dealt = mutableListOf<Card>()
        for (card in picked) {
            // 同一张牌生成两张，保证一定能配对
            dealt.add(Card(card.suitString(), card.rankString()))
            dealt.add(Card(card.suitString(), card.rankString()))
        }
        // 洗牌：随机打乱位置
        for (i in dealt.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = dealt[i]
            dealt[i] = dealt[j]
            dealt[j] = tmp
        }
        cards.clear()
        cards.addAll(dealt)
    }

    fun cardAtIndex(index: Int): Card {
        return cards.get(index)
    }

    fun chooseCardAtIndex(index: Int) {
        val card = cardAtIndex(index)
        if (!card.isMatched) {
            if (card.isChosen) {
                card.isChosen = false
            } else {
                for (otherCard in cards) {
                    if (otherCard.isChosen && !otherCard.isMatched) {
                        val matchScore = card.match(arrayOf(otherCard))
                        if (matchScore > 0) {
                            score += matchScore * MATCH_BONUS
                            otherCard.isMatched = true
                            card.isMatched = true
                        } else {
                            score -= MISMATCH_PENALTY
                            otherCard.isChosen = false
                        }
                        break
                    }
                }
                score -= COST_TO_CHOOSE
                card.isChosen = true
            }
        }
    }

    /** 还有多少张牌没有配对成功。 */
    fun unmatchedCount(): Int = cards.count { !it.isMatched }

    /** 是否已经全部配对完成（一局结束）。 */
    fun isGameOver(): Boolean = unmatchedCount() == 0

    /** 重新开始一局：重新发牌并清零分数。 */
    fun restart() {
        deal()
        score = 0
    }
}
