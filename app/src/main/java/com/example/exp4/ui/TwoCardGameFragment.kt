package com.example.exp4.ui

import com.example.exp4.R

/**
 * 第 2 个 Fragment：2 张牌的翻牌游戏（两张点数相同的牌配对）。
 */
class TwoCardGameFragment : GameFragment() {

    override val groupSize: Int = 2
    override val cardCountRes: Int = R.integer.game_two_card_count
    override val columnsRes: Int = R.integer.game_two_card_columns
    override val stateFileName: String = "game_two_card.dat"
}
