package com.example.exp4.ui

import com.example.exp4.R

/**
 * 第 3 个 Fragment：3 张牌的翻牌游戏（三张点数相同的牌配对）。
 */
class ThreeCardGameFragment : GameFragment() {

    override val groupSize: Int = 3
    override val cardCountRes: Int = R.integer.game_three_card_count
    override val columnsRes: Int = R.integer.game_three_card_columns
    override val stateFileName: String = "game_three_card.dat"
}
