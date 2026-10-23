package com.example.exp4

import com.example.exp4.model.CardMatchingGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 游戏模型的单元测试：验证三张同点数的发牌、配对、计分与重置。
 *
 * 竖屏使用 4 列 × 6 行共 24 张牌（8 组三张同点数）。
 */
class CardMatchingGameTest {

    /** 与竖屏资源 game_card_count 保持一致。 */
    private val cardCount = 24

    /** 找出三张点数相同的牌的索引。 */
    private fun findTriple(game: CardMatchingGame): Triple<Int, Int, Int> {
        for (i in 0 until game.cardCount) {
            if (game.cardAtIndex(i).isMatched) continue
            for (j in i + 1 until game.cardCount) {
                if (game.cardAtIndex(j).isMatched) continue
                if (game.cardAtIndex(i).rankString() != game.cardAtIndex(j).rankString()) continue
                for (k in j + 1 until game.cardCount) {
                    if (game.cardAtIndex(k).isMatched) continue
                    val cards = arrayOf(game.cardAtIndex(i), game.cardAtIndex(j))
                    if (game.cardAtIndex(k).match(cards) > 0) {
                        return Triple(i, j, k)
                    }
                }
            }
        }
        throw AssertionError("没有找到三张点数相同的牌")
    }

    /** 找出三张点数互不相同的牌（必定配对失败）。 */
    private fun findMismatch(game: CardMatchingGame): Triple<Int, Int, Int> {
        for (i in 0 until game.cardCount) {
            val rankA = game.cardAtIndex(i).rankString()
            for (j in 0 until game.cardCount) {
                val rankB = game.cardAtIndex(j).rankString()
                if (i == j || rankA == rankB) continue
                for (k in 0 until game.cardCount) {
                    val rankC = game.cardAtIndex(k).rankString()
                    if (k == i || k == j || rankC == rankA || rankC == rankB) continue
                    return Triple(i, j, k)
                }
            }
        }
        throw AssertionError("没有找到三张点数互不相同的牌")
    }

    @Test
    fun deal_createsRequestedNumberOfCards() {
        val game = CardMatchingGame(cardCount)
        assertEquals(cardCount, game.cardCount)
        assertEquals(0, game.score)
        assertFalse(game.isGameOver())
        assertEquals(cardCount, game.unmatchedCount())
    }

    @Test
    fun everyCardBelongsToATriple() {
        repeat(50) {
            val game = CardMatchingGame(cardCount)
            val rankCounts = mutableMapOf<String, Int>()
            for (i in 0 until game.cardCount) {
                val rank = game.cardAtIndex(i).rankString()
                rankCounts[rank] = (rankCounts[rank] ?: 0) + 1
            }
            // 每张牌都属于一组“三张同点数”，因此每个点数出现的次数都是 3
            assertTrue(rankCounts.isNotEmpty())
            assertTrue("存在无法配对的牌：$rankCounts", rankCounts.values.all { it == 3 })
            assertEquals(cardCount / 3, rankCounts.size)
        }
    }

    @Test
    fun perfectPlay_finishesTheGameWithPositiveScore() {
        repeat(50) {
            val game = CardMatchingGame(cardCount)
            val matched = BooleanArray(game.cardCount)
            while (!game.isGameOver()) {
                val (a, b, c) = findTriple(game)
                game.chooseCardAtIndex(a)
                game.chooseCardAtIndex(b)
                game.chooseCardAtIndex(c)
                assertTrue(game.cardAtIndex(a).isMatched)
                assertTrue(game.cardAtIndex(b).isMatched)
                assertTrue(game.cardAtIndex(c).isMatched)
                matched[a] = true
                matched[b] = true
                matched[c] = true
            }
            assertTrue("全部配对后游戏应该结束", game.isGameOver())
            assertTrue(matched.all { it })
            // 每组配对成功得分 4 × MATCH_BONUS(4) = 16 分，每翻开一张牌扣 COST_TO_CHOOSE(1) 分。
            // 8 组共 8 × 16 = 128 分，一局共翻开 24 张牌扣 24 分，合计 104 分。
            assertEquals(104, game.score)
        }
    }

    @Test
    fun mismatch_appliesPenalty() {
        val game = CardMatchingGame(cardCount)
        val (a, b, c) = findMismatch(game)
        game.chooseCardAtIndex(a)   // 翻牌 -1
        game.chooseCardAtIndex(b)   // 翻牌 -1
        game.chooseCardAtIndex(c)   // 翻牌 -1、配对失败 -2
        assertEquals(-5, game.score)
        // 配对失败后：前两张牌恢复为未选中，刚点的那张仍是选中状态
        assertFalse(game.cardAtIndex(a).isChosen)
        assertFalse(game.cardAtIndex(b).isChosen)
        assertTrue(game.cardAtIndex(c).isChosen)
        assertFalse(game.cardAtIndex(a).isMatched)
        assertFalse(game.cardAtIndex(c).isMatched)
    }

    @Test
    fun reset_clearsScoreAndDealsNewCards() {
        val game = CardMatchingGame(cardCount)
        val (a, b, c) = findTriple(game)
        game.chooseCardAtIndex(a)
        game.chooseCardAtIndex(b)
        game.chooseCardAtIndex(c)
        game.reset()
        assertEquals(0, game.score)
        assertEquals(cardCount, game.cardCount)
        assertEquals(0, game.chosenCardCount())
        assertEquals(cardCount, game.unmatchedCount())
        assertFalse(game.isGameOver())
    }
}
