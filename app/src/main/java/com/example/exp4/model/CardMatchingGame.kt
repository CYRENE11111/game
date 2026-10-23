package com.example.exp4.model

import java.util.Collections
import java.util.Random

/**
 * 翻牌配对游戏（游戏模型 Model）。
 *
 * 三张牌玩法：连续翻开三张点数相同的牌即配对成功。
 * 计分规则：三张牌配对成功 +4 × 4 = 16 分，配对失败 -2 分，每翻开一张牌 -1 分。
 *
 * 发牌方式：按点数把整副牌分组，每个点数随机抽三张（三种花色）作为一组，
 * 保证每张牌都属于自己的“三张同点数”组合，一局游戏必定可以完成。
 *
 * @param count 一局游戏中牌的数量（会自动调整为 3 的倍数）。
 */
class CardMatchingGame(count: Int) {

    companion object {
        /** 一组配对需要的牌数：三张同点数的牌。 */
        const val GROUP_SIZE = 3

        /** 一副牌最多能组成的组数：13 个点数各一组。 */
        private const val MAX_GROUP_COUNT = 13
    }

    var score = 0
        private set

    /** 本局一共用了几张牌（3 的倍数）。 */
    val cardCount: Int = ((count.coerceAtLeast(GROUP_SIZE) + GROUP_SIZE - 1) / GROUP_SIZE * GROUP_SIZE)
        .coerceAtMost(MAX_GROUP_COUNT * GROUP_SIZE)

    private val cards: MutableList<Card> = mutableListOf()

    private val random = Random()

    val MISMATCH_PENALTY = 2
    val MATCH_BONUS = 4
    val COST_TO_CHOOSE = 1

    init {
        deal()
    }

    /** 使用附件中的牌堆按点数分组抽牌，保证每组都是三张同点数的牌。 */
    private fun deal() {
        val deck = Deck()
        val cardsByRank = LinkedHashMap<String, MutableList<Card>>()
        while (true) {
            val card = deck.drawRandomCard() ?: break
            cardsByRank.getOrPut(card.rankString()) { mutableListOf() }.add(card)
        }

        val ranks = cardsByRank.keys.toMutableList()
        Collections.shuffle(ranks, random)

        val dealt = mutableListOf<Card>()
        for (rank in ranks) {
            if (dealt.size >= cardCount) break
            val sameRankCards = cardsByRank.getValue(rank)
            Collections.shuffle(sameRankCards, random)
            // 同一点数的四张牌中随机取三张（三种花色），复制成一组“三张同点数”的牌
            for (i in 0 until GROUP_SIZE) {
                val source = sameRankCards[i]
                dealt.add(Card(source.suitString(), source.rankString()))
            }
        }

        // 洗牌：随机打乱位置
        Collections.shuffle(dealt, random)
        cards.clear()
        cards.addAll(dealt)
    }

    fun cardAtIndex(index: Int): Card {
        return cards.get(index)
    }

    /**
     * 翻开（或取消选择）一张牌，并完成三张牌的配对判定。
     *
     * 规则：
     * 1. 已配对的牌不能再选；
     * 2. 再次点击已翻开的牌表示取消选择；
     * 3. 已有两张牌处于翻开状态时，第三张牌与它们点数相同即配对成功，
     *    否则配对失败，前两张牌翻回背面。
     */
    fun chooseCardAtIndex(index: Int) {
        val card = cardAtIndex(index)
        if (card.isMatched) return

        if (card.isChosen) {
            card.isChosen = false
            return
        }

        val otherChosenCards = cards.filter { it.isChosen && !it.isMatched }

        if (otherChosenCards.size >= GROUP_SIZE - 1) {
            val matchScore = card.match(otherChosenCards.take(GROUP_SIZE - 1).toTypedArray())
            if (matchScore > 0) {
                score += matchScore * MATCH_BONUS
                for (otherCard in otherChosenCards) {
                    otherCard.isMatched = true
                }
                card.isMatched = true
            } else {
                score -= MISMATCH_PENALTY
                for (otherCard in otherChosenCards) {
                    otherCard.isChosen = false
                }
            }
        }

        score -= COST_TO_CHOOSE
        card.isChosen = true
    }

    /** 当前已翻开、但还没有配对成功的牌数（0 ~ 2）。 */
    fun chosenCardCount(): Int = cards.count { it.isChosen && !it.isMatched }

    /** 还有多少张牌没有配对成功。 */
    fun unmatchedCount(): Int = cards.count { !it.isMatched }

    /** 是否已经全部配对完成（一局结束）。 */
    fun isGameOver(): Boolean = unmatchedCount() == 0

    /** 重置游戏：重新发牌并清零分数（实验要求的 reset 方法）。 */
    fun reset() {
        score = 0
        deal()
    }
}
