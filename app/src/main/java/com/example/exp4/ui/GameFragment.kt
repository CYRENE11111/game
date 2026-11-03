package com.example.exp4.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exp4.R
import com.example.exp4.model.Card
import com.example.exp4.model.CardMatchingGame
import com.example.exp4.storage.GameStore
import com.example.exp4.storage.PlayerRepository
import com.example.exp4.view.CardAdapter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 翻牌游戏 Fragment 的公共实现。
 *
 * 子类只需要指定“几张牌配对”和界面资源：
 * [TwoCardGameFragment] 两张牌配对，[ThreeCardGameFragment] 三张牌配对。
 *
 * 职责：接受点击、调用模型、同步 RecyclerView；
 * 另外每次操作后都会把整个 [CardMatchingGame] 对象序列化写入文件，
 * 退出程序后重新打开即可恢复上一次的游戏进度。
 */
abstract class GameFragment : Fragment(), CardAdapter.OnCardClickListener {

    /** 一组配对需要的牌数：2 或 3。 */
    protected abstract val groupSize: Int

    /** 一局游戏的牌数（integer 资源）。 */
    protected abstract val cardCountRes: Int

    /** 网格列数（integer 资源，横竖屏不同）。 */
    protected abstract val columnsRes: Int

    /** 本玩法保存游戏进度的文件名。 */
    protected abstract val stateFileName: String

    private lateinit var game: CardMatchingGame
    private lateinit var adapter: CardAdapter
    private lateinit var rvCards: RecyclerView
    private lateinit var tvScore: TextView
    private lateinit var tvBest: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var tvStatus: TextView

    /** 动画进行中时锁定交互，避免牌面状态错乱。 */
    private var locked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvCards = view.findViewById(R.id.rvCards)
        tvScore = view.findViewById(R.id.tvScore)
        tvBest = view.findViewById(R.id.tvBest)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        tvStatus = view.findViewById(R.id.tvStatus)

        // 先尝试从文件恢复上一局，文件不存在时重新发牌
        val saved = GameStore.load(requireContext(), stateFileName, groupSize)
        game = saved?.game ?: CardMatchingGame(resources.getInteger(cardCountRes), groupSize)

        setupRecyclerView()

        view.findViewById<MaterialButton>(R.id.btnRestart).setOnClickListener { restartGame() }
        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveProgress(true) }

        refresh()
        tvStatus.text = when {
            saved == null -> getString(R.string.game_status_ready)
            game.isGameOver() -> getString(R.string.game_status_win, game.score)
            else -> getString(R.string.game_status_restored, formatTime(saved.savedAt))
        }
    }

    override fun onPause() {
        super.onPause()
        // 切换到其他 Fragment、按 Home 键或退出程序前都保存一次
        saveProgress(false)
    }

    /** 创建 RecyclerView：网格布局 + 卡牌适配器。 */
    private fun setupRecyclerView() {
        adapter = CardAdapter(this)
        rvCards.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(columnsRes))
        rvCards.adapter = adapter
        // 翻牌 / 配对动画由 CardView 自己完成，关闭默认的列表动画避免相互干扰
        rvCards.itemAnimator = null
        rvCards.setHasFixedSize(true)
        adapter.submitCards(cardsOfGame())
    }

    private fun cardsOfGame(): List<Card> =
        (0 until game.cardCount).map { game.cardAtIndex(it) }

    /**
     * 用户点击一张牌：先交给模型处理，再通过适配器把模型结果同步到界面，
     * 最后把最新的游戏对象写入文件。
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
            saveProgress(false)
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
                // 配对成功：参与配对的牌一起播放高亮动画
                adapter.updateCard(position)
                chosenBefore.forEach { adapter.updateCard(it) }
                updateStatus(matched = true)
                val newRecord = updateScoreAndRemaining()
                saveProgress(false)
                if (game.isGameOver()) {
                    finishGame(newRecord)
                }
            }

            chosenBefore.size >= game.groupSize - 1 -> {
                // 配对失败：先把参与的牌都翻开让玩家看到，再一起翻回去
                locked = true
                adapter.setRevealedPositions((chosenBefore + position).toSet())
                updateStatus(mismatch = true)
                updateScoreAndRemaining()
                saveProgress(false)
                rvCards.postDelayed({
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
                saveProgress(false)
            }
        }
    }

    /** 同步分数、最高分与剩余张数。 */
    private fun refresh() {
        tvScore.text = game.score.toString()
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
        tvRemaining.text = getString(R.string.remaining_value, game.unmatchedCount())
    }

    /**
     * 刷新分数与剩余张数，并同步历史最高分。
     *
     * @return 本局分数是否刷新了最高分。
     */
    private fun updateScoreAndRemaining(): Boolean {
        tvScore.text = game.score.toString()
        tvRemaining.text = getString(R.string.remaining_value, game.unmatchedCount())
        val newRecord = PlayerRepository.updateHighScore(game.score)
        if (newRecord) {
            // 最高分也属于需要持久化的历史信息
            PlayerRepository.save(requireContext())
        }
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
        return newRecord
    }

    private fun updateStatus(matched: Boolean = false, mismatch: Boolean = false) {
        tvStatus.text = when {
            matched -> getString(R.string.game_status_matched)
            mismatch -> getString(R.string.game_status_mismatch)
            else -> {
                val chosen = game.chosenCardCount()
                if (chosen == 0) {
                    getString(R.string.game_status_ready)
                } else {
                    getString(R.string.game_status_pick_more, chosen, game.groupSize - chosen)
                }
            }
        }
    }

    /** 一局结束：显示本局得分与最高分。 */
    private fun finishGame(newRecord: Boolean) {
        tvBest.text = getString(R.string.label_best, PlayerRepository.player.highScore)
        tvStatus.text = if (newRecord) {
            getString(R.string.game_status_win_record, game.score)
        } else {
            getString(R.string.game_status_win, game.score)
        }
    }

    /** 重置游戏：模型重新发牌，界面全部重置，并覆盖保存到文件。 */
    private fun restartGame() {
        locked = false
        game.reset()
        adapter.submitCards(cardsOfGame())
        refresh()
        tvStatus.text = getString(R.string.game_status_restart)
        saveProgress(false)
    }

    /** 把当前游戏对象写入文件；[showToast] 为 true 时给出手动保存提示。 */
    private fun saveProgress(showToast: Boolean) {
        if (!isAdded || !::game.isInitialized) return
        val success = GameStore.save(requireContext(), stateFileName, game)
        if (showToast) {
            val message = if (success) R.string.toast_game_saved else R.string.toast_game_save_failed
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTime(time: Long): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(time))
    }
}
