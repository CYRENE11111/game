package com.example.exp4.model

import java.io.Serializable
import java.util.Collections
import java.util.Random

/**
 * 翻牌配对游戏（游戏模型 Model）。
 *
 * 玩法：连续翻开 [groupSize] 张点数相同的牌即配对成功。
 * 计分规则：配对成功 +4 × 4 = 16 分，配对失败 -2 分，每翻开一张牌 -1 分。
 *
 * 发牌方式：按点数把整副牌分组，每个点数随机抽 [groupSize] 张（不同花色）作为一组，
 * 保证每张牌都属于自己的“同点数”组合，一局游戏必定可以完成。
 *
 * 实现 [Serializable]：整个游戏对象（牌面、选中状态、得分）都可以通过对象流
 * 写入文件，下次打开时直接恢复上一次的游戏进度。
 *
 * @param count     一局游戏中牌的数量（会自动调整为 [groupSize] 的倍数）。
 * @param groupSize 一组配对需要的牌数：2 表示两张牌配对，3 表示三张牌配对。
 */
class CardMatchingGame(
    count: Int,
    val groupSize: Int = DEFAULT_GROUP_SIZE
) : Serializable {

    companion object {
        /** 默认玩法：三张同点数的牌配对。 */
        const val DEFAULT_GROUP_SIZE = 3

        /** 一副牌最多能组成的组数：13 个点数各一组。 */
        private const val MAX_GROUP_COUNT = 13

        private const val serialVersionUID = 1L
    }

    var score = 0
        private set

    /** 本局一共用了几张牌（[groupSize] 的倍数）。 */
    val cardCount: Int =
        ((count.coerceAtLeast(groupSize) + groupSize - 1) / groupSize * groupSize)
            .coerceAtMost(MAX_GROUP_COUNT * groupSize)

    private val cards: MutableList<Card> = mutableListOf()

    private val random = Random()

    private val MISMATCH_PENALTY = 2
    private val MATCH_BONUS = 4
    private val COST_TO_CHOOSE = 1

    init {
        require(groupSize in 2..4) { "groupSize 必须在 2~4 之间" }
        deal()
    }

    /** 使用附件中的牌堆按点数分组抽牌，保证每组都是同点数的牌。 */
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
            // 同一点数的四张牌中随机取 groupSize 张，复制成一组“同点数”的牌
            for (i in 0 until groupSize) {
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
     * 翻开（或取消选择）一张牌，并完成 [groupSize] 张牌的配对判定。
     *
     * 规则：
     * 1. 已配对的牌不能再选；
     * 2. 再次点击已翻开的牌表示取消选择；
     * 3. 已有 groupSize - 1 张牌处于翻开状态时，新牌与它们点数相同即配对成功，
     *    否则配对失败，之前翻开的牌翻回背面。
     */
    fun chooseCardAtIndex(index: Int) {
        val card = cardAtIndex(index)
        if (card.isMatched) return

        if (card.isChosen) {
            card.isChosen = false
            return
        }

        val otherChosenCards = cards.filter { it.isChosen && !it.isMatched }

        if (otherChosenCards.size >= groupSize - 1) {
            val matchScore = card.match(otherChosenCards.take(groupSize - 1).toTypedArray())
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

    /** 当前已翻开、但还没有配对成功的牌数（0 ~ groupSize - 1）。 */
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
