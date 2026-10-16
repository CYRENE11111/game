package com.example.exp4

import com.example.exp4.model.CardMatchingGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 游戏模型的单元测试：验证发牌、配对、计分与重新开始。
 *
 * 界面参照《翻牌游戏.pdf》使用 4 列 × 4 行共 16 张牌（8 对）。
 */
class CardMatchingGameTest {

    /** 与 GameActivity 中的 CARD_COUNT 保持一致。 */
    private val cardCount = 16

    @Test
    fun deal_createsRequestedNumberOfCards() {
        val game = CardMatchingGame(cardCount)
        assertEquals(cardCount, game.cardCount)
        assertEquals(0, game.score)
        assertFalse(game.isGameOver())
    }

    @Test
    fun everyCardHasExactlyOneMatchingPartner() {
        repeat(50) {
            val game = CardMatchingGame(cardCount)
            var pairs = 0
            val used = BooleanArray(game.cardCount)
            for (i in 0 until game.cardCount) {
                if (used[i]) continue
                val card = game.cardAtIndex(i)
                var partner = -1
                for (j in i + 1 until game.cardCount) {
                    if (used[j]) continue
                    if (card.match(arrayOf(game.cardAtIndex(j))) == 4) {
                        partner = j
                        break
                    }
                }
                assertTrue("牌 $i 没有找到配对的牌", partner >= 0)
                used[i] = true
                used[partner] = true
                pairs++
            }
            assertEquals(game.cardCount / 2, pairs)
        }
    }

    @Test
    fun perfectPlay_finishesTheGameWithPositiveScore() {
        repeat(50) {
            val game = CardMatchingGame(cardCount)
            // 用配对成功最小的翻牌次数完成一局：每对牌翻开两次
            val matched = BooleanArray(game.cardCount)
            for (i in 0 until game.cardCount) {
                if (matched[i]) continue
                var partner = -1
                for (j in i + 1 until game.cardCount) {
                    if (matched[j]) continue
                    if (game.cardAtIndex(i).match(arrayOf(game.cardAtIndex(j))) == 4) {
                        partner = j
                        break
                    }
                }
                assertTrue(partner >= 0)
                game.chooseCardAtIndex(i)
                game.chooseCardAtIndex(partner)
                matched[i] = true
                matched[partner] = true
            }
            assertTrue("全部配对后游戏应该结束", game.isGameOver())
            // 每对配对成功得分 4 × MATCH_BONUS(4) = 16 分，每翻开一张牌扣 COST_TO_CHOOSE(1) 分。
            // 8 对共 8 × 16 = 128 分，一局共翻开 16 张牌扣 16 分，合计 112 分。
            assertEquals(112, game.score)
        }
    }

    @Test
    fun mismatch_appliesPenalty() {
        val game = CardMatchingGame(cardCount)
        // 找两张点数不同、花色不同（必定配对失败）的牌
        var a = 0
        var b = 1
        outer@ for (i in 0 until game.cardCount) {
            for (j in i + 1 until game.cardCount) {
                if (game.cardAtIndex(i).match(arrayOf(game.cardAtIndex(j))) == 0) {
                    a = i
                    b = j
                    break@outer
                }
            }
        }
        game.chooseCardAtIndex(a)   // 翻牌 -1
        game.chooseCardAtIndex(b)   // 翻牌 -1、配对失败 -2
        assertEquals(-4, game.score)
        // 配对失败后：先翻开的那张牌恢复为未选中，刚点的那张仍是选中状态
        assertFalse(game.cardAtIndex(a).isChosen)
        assertTrue(game.cardAtIndex(b).isChosen)
        assertFalse(game.cardAtIndex(a).isMatched)
        assertFalse(game.cardAtIndex(b).isMatched)
    }

    @Test
    fun restart_resetsScoreAndUnmatchedCount() {
        val game = CardMatchingGame(cardCount)
        game.chooseCardAtIndex(0)
        game.chooseCardAtIndex(1)
        game.restart()
        assertEquals(0, game.score)
        assertEquals(cardCount, game.unmatchedCount())
        assertFalse(game.isGameOver())
    }
}
