package com.example.exp4

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.exp4.model.CardMatchingGame
import com.example.exp4.model.PlayerRepository
import com.example.exp4.view.CardBoardView
import com.example.exp4.view.CardView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * 翻牌游戏 Activity —— 控制器（Controller）。
 *
 * 职责：
 * 1. 接受用户交互（点击卡牌、“重置游戏”、“退出”）；
 * 2. 调用游戏模型 [CardMatchingGame] 处理游戏逻辑；
 * 3. 把模型的最新状态同步到界面（View）上；
 * 4. 可以重新开始一局游戏（重新发牌、清零分数）。
 *
 * 界面参照《翻牌游戏.pdf》：4 列 × 4 行共 16 张牌，底部为“得分”与“重置游戏 / 退出”。
 */
class GameActivity : AppCompatActivity() {

    companion object {
        /** 一局游戏的牌数：4 列 × 4 行 = 16 张（8 对）。 */
        private const val CARD_COUNT = 16
        private const val COLUMNS = 4
    }

    private lateinit var game: CardMatchingGame
    private lateinit var board: CardBoardView
    private lateinit var tvScore: TextView
    private lateinit var tvBest: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvStatus: TextView

    private val cardViews = mutableListOf<CardView>()

    /** 动画进行中时锁定交互，避免牌面状态错乱。 */
    private var locked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_card_game)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gameRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarGame)
        toolbar.subtitle = PlayerRepository.player.name

        board = findViewById(R.id.board)
        tvScore = findViewById(R.id.tvScore)
        tvBest = findViewById(R.id.tvBest)
        tvRemaining = findViewById(R.id.tvRemaining)
        tvStatus = findViewById(R.id.tvStatus)

        game = CardMatchingGame(CARD_COUNT)
        buildBoard()

        findViewById<MaterialButton>(R.id.btnRestart).setOnClickListener { restartGame() }
        findViewById<MaterialButton>(R.id.btnExit).setOnClickListener { finish() }

        refresh()                                     // 同步视图：分数、剩余张数、最高分
        tvStatus.text = getString(R.string.game_status_ready)
    }

    /** 根据模型的牌数创建界面上的卡牌控件。 */
    private fun buildBoard() {
        board.removeAllViews()
        cardViews.clear()
        board.columns = COLUMNS
        board.rows = CARD_COUNT / COLUMNS

        for (index in 0 until game.cardCount) {
            val card = game.cardAtIndex(index)
            val cardView = CardView(this)
            cardView.bind(card.suitString(), card.rankString(), card.isRedSuit())
            cardView.render(false, false, false)      // 初始全部背面朝上
            cardView.setOnClickListener { onCardClicked(index) }
            board.addView(cardView)
            cardViews.add(cardView)
        }
    }

    /**
     * 用户点击一张牌：先交给模型处理，再把模型结果同步到界面。
     */
    private fun onCardClicked(index: Int) {
        if (locked || index !in cardViews.indices) return

        val card = game.cardAtIndex(index)
        if (card.isMatched) return                    // 已配对的牌不可再选

        // 记录操作前的状态，用来判断这次是配对成功还是失败
        val wasChosen = card.isChosen
        val hadPrevChosen = hasOtherChosenCard(index)

        // 交给模型处理
        game.chooseCardAtIndex(index)

        if (wasChosen) {
            // 取消选择：直接翻回背面
            syncCard(index)
            updateStatus()
            updateScoreAndRemaining()
            return
        }

        val isMatched = card.isMatched
        val isMismatch = hadPrevChosen && !isMatched && !hasOtherChosenCard(index)

        when {
            isMatched -> {
                // 配对成功：两张牌翻开并高亮
                refreshMatchedCards(index)
                updateStatus(matched = true)
                updateScoreAndRemaining()
                if (game.isGameOver()) {
                    finishGame()
                }
            }

            isMismatch -> {
                // 配对失败：先把两张牌都翻开让玩家看到，再一起翻回去
                locked = true
                cardViews[index].flipUp(selected = false, matched = false)
                updateStatus(mismatch = true)
                updateScoreAndRemaining()
                cardViews[index].postDelayed({
                    // 依据模型的最终状态重建视图，保证界面与模型一致
                    for (i in cardViews.indices) {
                        syncCard(i)
                    }
                    locked = false
                    updateStatus()
                }, 900L)
            }

            else -> {
                // 普通翻开一张牌：分数 -1
                syncCard(index)
                updateStatus()
                updateScoreAndRemaining()
            }
        }
    }

    /** 配对成功的两张牌一起播放高亮动画。 */
    private fun refreshMatchedCards(justClicked: Int) {
        for (i in cardViews.indices) {
            val card = game.cardAtIndex(i)
            if (card.isMatched && cardViews[i].isFaceUp) {
                cardViews[i].playMatched(selected = false)
            }
        }
        val view = cardViews[justClicked]
        view.flipUp(selected = false, matched = false) {
            view.playMatched(selected = false)
        }
    }

    /** 除 index 之外，是否还有一张已翻开但尚未配对的牌。 */
    private fun hasOtherChosenCard(index: Int): Boolean {
        for (i in 0 until game.cardCount) {
            if (i == index) continue
            val c = game.cardAtIndex(i)
            if (c.isChosen && !c.isMatched) return true
        }
        return false
    }

    /** 把某一张牌同步成模型的当前状态（不带翻牌动画）。 */
    private fun syncCard(index: Int) {
        val card = game.cardAtIndex(index)
        val view = cardViews[index]
        view.render(card.isChosen || card.isMatched, card.isChosen, card.isMatched)
    }

    /** 同步所有牌、分数、最高分与剩余张数。 */
    private fun refresh() {
        for (i in cardViews.indices) {
            syncCard(i)
        }
        tvScore.text = game.score.toString()
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
        tvRemaining.text = getString(R.string.remaining_value, game.unmatchedCount())
    }

    private fun updateScoreAndRemaining() {
        tvScore.text = game.score.toString()
        tvRemaining.text = getString(R.string.remaining_value, game.unmatchedCount())
        // 实时刷新最高分
        PlayerRepository.updateHighScore(game.score)
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
    }

    private fun updateStatus(matched: Boolean = false, mismatch: Boolean = false) {
        tvStatus.text = when {
            matched -> getString(R.string.game_status_matched)
            mismatch -> getString(R.string.game_status_mismatch)
            else -> if (game.unmatchedCount() == game.cardCount) {
                getString(R.string.game_status_ready)
            } else {
                getString(R.string.game_status_pick_one)
            }
        }
    }

    /** 一局结束：统计成绩并更新最高分。 */
    private fun finishGame() {
        val oldBest = PlayerRepository.player.highScore
        PlayerRepository.updateHighScore(game.score)
        val newRecord = game.score > oldBest
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
        tvStatus.text = if (newRecord) {
            getString(R.string.game_status_win_record, game.score)
        } else {
            getString(R.string.game_status_win, game.score)
        }
    }

    /** 重置游戏：模型重新发牌，界面全部重置。 */
    private fun restartGame() {
        locked = false
        game.restart()
        for (i in cardViews.indices) {
            val card = game.cardAtIndex(i)
            val view = cardViews[i]
            view.animate().cancel()
            view.bind(card.suitString(), card.rankString(), card.isRedSuit())
            view.render(false, false, false)
        }
        refresh()
        tvStatus.text = getString(R.string.game_status_restart)
    }
}
