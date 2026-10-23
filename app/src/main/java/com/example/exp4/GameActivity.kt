package com.example.exp4

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exp4.model.Card
import com.example.exp4.model.CardMatchingGame
import com.example.exp4.model.PlayerRepository
import com.example.exp4.view.CardAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * 翻牌游戏 Activity —— 控制器（Controller）。
 *
 * 职责：
 * 1. 接受用户交互（点击卡牌、“重置游戏”、“退出”）；
 * 2. 调用游戏模型 [CardMatchingGame] 处理游戏逻辑；
 * 3. 通过 [CardAdapter] 把模型的最新状态同步到 RecyclerView 上；
 * 4. 可以重新开始一局游戏（重新发牌、清零分数）。
 *
 * 牌数由资源限定符决定：竖屏 24 张（4 列 × 6 行），横屏 18 张（6 列 × 3 行），
 * 因此旋转屏幕后会自动使用与当前方向匹配的牌数。
 */
class GameActivity : AppCompatActivity(), CardAdapter.OnCardClickListener {

    private lateinit var game: CardMatchingGame
    private lateinit var adapter: CardAdapter
    private lateinit var rvCards: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvBest: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvStatus: TextView

    /** 动画进行中时锁定交互，避免牌面状态错乱。 */
    private var locked = false

    /** 当前方向下的牌数与列数（values / values-land 中的整数资源）。 */
    private val cardCount: Int get() = resources.getInteger(R.integer.game_card_count)
    private val columns: Int get() = resources.getInteger(R.integer.game_card_columns)

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

        rvCards = findViewById(R.id.rvCards)
        tvScore = findViewById(R.id.tvScore)
        tvBest = findViewById(R.id.tvBest)
        tvRemaining = findViewById(R.id.tvRemaining)
        tvStatus = findViewById(R.id.tvStatus)

        game = CardMatchingGame(cardCount)
        setupRecyclerView()

        findViewById<MaterialButton>(R.id.btnRestart).setOnClickListener { restartGame() }
        findViewById<MaterialButton>(R.id.btnExit).setOnClickListener { finish() }

        refresh()                                     // 同步视图：分数、剩余张数、最高分
        tvStatus.text = getString(R.string.game_status_ready)
    }

    /** 创建 RecyclerView：网格布局 + 卡牌适配器。 */
    private fun setupRecyclerView() {
        adapter = CardAdapter(this)
        rvCards.layoutManager = GridLayoutManager(this, columns)
        rvCards.adapter = adapter
        // 翻牌 / 配对动画由 CardView 自己完成，关闭默认的列表动画避免相互干扰
        rvCards.itemAnimator = null
        rvCards.setHasFixedSize(true)
        adapter.submitCards(cardsOfGame())
    }

    private fun cardsOfGame(): List<Card> =
        (0 until game.cardCount).map { game.cardAtIndex(it) }

    /**
     * 用户点击一张牌：先交给模型处理，再通过适配器把模型结果同步到界面。
     */
    override fun onCardClicked(position: Int) {
        if (locked || position !in 0 until game.cardCount) return

        val card = game.cardAtIndex(position)
        if (card.isMatched) return                    // 已配对的牌不可再选

        // 取消选择：直接翻回背面
        if (card.isChosen) {
            game.chooseCardAtIndex(position)
            adapter.updateCard(position)
            updateStatus()
            updateScoreAndRemaining()
            return
        }

        // 记录操作前已翻开的牌，用来判断这次是配对成功还是失败
        val chosenBefore = (0 until game.cardCount).filter {
            val c = game.cardAtIndex(it)
            c.isChosen && !c.isMatched
        }

        // 交给模型处理
        game.chooseCardAtIndex(position)

        when {
            card.isMatched -> {
                // 三张牌配对成功：三张牌一起播放高亮动画
                adapter.updateCard(position)
                chosenBefore.forEach { adapter.updateCard(it) }
                updateStatus(matched = true)
                updateScoreAndRemaining()
                if (game.isGameOver()) {
                    finishGame()
                }
            }

            chosenBefore.size >= CardMatchingGame.GROUP_SIZE - 1 -> {
                // 配对失败：先把三张牌都翻开让玩家看到，再一起翻回去
                locked = true
                adapter.setRevealedPositions((chosenBefore + position).toSet())
                updateStatus(mismatch = true)
                updateScoreAndRemaining()
                rvCards.postDelayed({
                    // 依据模型的最终状态重建视图，保证界面与模型一致
                    adapter.setRevealedPositions(emptySet())
                    locked = false
                    updateStatus()
                }, 900L)
            }

            else -> {
                // 普通翻开一张牌：分数 -1
                adapter.updateCard(position)
                updateStatus()
                updateScoreAndRemaining()
            }
        }
    }

    /** 同步分数、最高分与剩余张数。 */
    private fun refresh() {
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
            else -> when (game.chosenCardCount()) {
                0 -> getString(R.string.game_status_ready)
                1 -> getString(R.string.game_status_pick_one)
                else -> getString(R.string.game_status_pick_two)
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
        game.reset()
        adapter.submitCards(cardsOfGame())
        refresh()
        tvStatus.text = getString(R.string.game_status_restart)
    }
}
