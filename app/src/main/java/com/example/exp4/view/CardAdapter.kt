package com.example.exp4.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.exp4.R
import com.example.exp4.model.Card

/**
 * 游戏界面适配器（Adapter）：把游戏模型中的 [Card] 列表绑定到 RecyclerView 上。
 *
 * 职责：
 * 1. 创建 / 复用卡牌 ViewHolder（[CardViewHolder]）；
 * 2. 根据模型状态显示牌面、选中高亮、配对完成效果；
 * 3. 把用户对卡牌的点击通过 [OnCardClickListener] 回调给控制器（Activity）。
 */
class CardAdapter(
    private val listener: OnCardClickListener
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    /** 卡牌点击回调，由控制器（GameActivity）实现。 */
    interface OnCardClickListener {
        fun onCardClicked(position: Int)
    }

    private var cards: List<Card> = emptyList()

    /** 配对失败时临时展示牌面的位置集合。 */
    private var revealedPositions: Set<Int> = emptySet()

    private companion object {
        /** 普通状态刷新（带翻牌动画）使用的 payload。 */
        const val PAYLOAD_REFRESH = "payload_refresh"
    }

    /** 重新提交一局游戏的牌，界面整体刷新。 */
    fun submitCards(newCards: List<Card>) {
        cards = newCards.toList()
        revealedPositions = emptySet()
        notifyDataSetChanged()
    }

    /** 某张牌的状态变化后刷新该位置（带翻牌 / 配对动画）。 */
    fun updateCard(position: Int) {
        notifyItemChanged(position, PAYLOAD_REFRESH)
    }

    /** 配对失败时，把参与本次配对的三张牌短暂翻开。 */
    fun setRevealedPositions(positions: Set<Int>) {
        revealedPositions = positions
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false) as CardView
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        bindCard(holder.cardView, position)
    }

    /**
     * 带 payload 的局部刷新：按模型的最终状态播放翻牌 / 配对动画。
     * 配对失败临时展示时走完整绑定，直接显示牌面。
     */
    override fun onBindViewHolder(holder: CardViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty() || position in revealedPositions) {
            bindCard(holder.cardView, position)
            return
        }

        val card = cards[position]
        when {
            card.isMatched -> {
                if (holder.cardView.isFaceUp) {
                    holder.cardView.playMatched(selected = false)
                } else {
                    holder.cardView.flipUp(selected = false, matched = false) {
                        holder.cardView.playMatched(selected = false)
                    }
                }
            }

            card.isChosen -> holder.cardView.flipUp(selected = true, matched = false)
            else -> holder.cardView.flipDown()
        }
    }

    /** 把某一张牌同步为模型的当前状态。 */
    private fun bindCard(view: CardView, position: Int) {
        val card = cards[position]
        val faceUp = card.isChosen || card.isMatched || position in revealedPositions
        view.bind(card.suitString(), card.rankString(), card.isRedSuit())
        view.render(faceUp, card.isChosen && !card.isMatched, card.isMatched)
    }

    /** 一张卡牌的 ViewHolder，负责把点击事件回调给控制器。 */
    inner class CardViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        init {
            cardView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCardClicked(position)
                }
            }
        }
    }
}
