package com.example.exp4.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import com.example.exp4.R

/**
 * 游戏界面（View）：一张扑克牌的显示与翻牌动画。
 *
 * 牌背朝上时显示统一的蓝色牌背图案；牌面朝上时显示“花色 + 点数”。
 * 配对成功的牌会变绿并淡化，被选中的牌有黄色描边。
 */
class CardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val FLIP_DURATION = 260L
        private const val MATCH_DURATION = 220L
        private const val MATCH_ALPHA = 0.55f
        private const val GAP_RATIO = 0.06f
    }

    private val backDrawable = ContextCompat.getDrawable(context, R.drawable.bg_card_back)
    private val faceDrawable = ContextCompat.getDrawable(context, R.drawable.bg_card_face)
    private val selectedDrawable = ContextCompat.getDrawable(context, R.drawable.bg_card_selected)
    private val matchedDrawable = ContextCompat.getDrawable(context, R.drawable.bg_card_matched)

    private val backBgColor = ContextCompat.getColor(context, R.color.card_back_bg)
    private val backInnerColor = ContextCompat.getColor(context, R.color.card_back_inner)
    private val backGridColor = ContextCompat.getColor(context, R.color.card_back_grid)
    private val backMarkColor = ContextCompat.getColor(context, R.color.card_back_mark)
    private val suitRedColor = ContextCompat.getColor(context, R.color.suit_red)
    private val suitBlackColor = ContextCompat.getColor(context, R.color.suit_black)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }

    private var backBitmap: Bitmap? = null
    private val backRect = Rect()

    private var suit = "♠"
    private var rank = "A"
    private var redSuit = false

    /** 牌面是否朝上。 */
    var isFaceUp: Boolean = false
        private set

    /** 绑定一张牌的显示内容（不改变朝向）。 */
    fun bind(cardSuit: String, cardRank: String, isRed: Boolean) {
        suit = cardSuit
        rank = cardRank
        redSuit = isRed
        invalidate()
    }

    /** 直接设置显示状态（用于重新开始 / 刷新界面，不带动画）。 */
    fun render(faceUp: Boolean, selected: Boolean, matched: Boolean) {
        animate().cancel()
        isFaceUp = faceUp
        rotationY = 0f
        scaleX = 1f
        scaleY = 1f
        alpha = if (matched) MATCH_ALPHA else 1f
        applyState(selected, matched)
    }

    private fun applyState(selected: Boolean, matched: Boolean) {
        background = when {
            matched -> matchedDrawable
            selected -> selectedDrawable
            isFaceUp -> faceDrawable
            else -> backDrawable
        }
        invalidate()
    }

    /** 翻牌：由牌背翻到牌面。 */
    fun flipUp(selected: Boolean, matched: Boolean, onEnd: (() -> Unit)? = null) {
        isFaceUp = true
        animate()
            .rotationY(90f)
            .setDuration(FLIP_DURATION / 2)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                applyState(selected, matched)
                animate()
                    .rotationY(0f)
                    .setDuration(FLIP_DURATION / 2)
                    .setInterpolator(LinearInterpolator())
                    .withEndAction { onEnd?.invoke() }
                    .start()
            }
            .start()
    }

    /** 翻牌：由牌面翻回牌背。 */
    fun flipDown(onEnd: (() -> Unit)? = null) {
        animate()
            .rotationY(90f)
            .setDuration(FLIP_DURATION / 2)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                isFaceUp = false
                applyState(false, false)
                animate()
                    .rotationY(0f)
                    .setDuration(FLIP_DURATION / 2)
                    .setInterpolator(LinearInterpolator())
                    .withEndAction { onEnd?.invoke() }
                    .start()
            }
            .start()
    }

    /** 配对成功：高亮变绿并淡化。 */
    fun playMatched(selected: Boolean, onEnd: (() -> Unit)? = null) {
        applyState(selected, true)
        animate()
            .alpha(MATCH_ALPHA)
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(MATCH_DURATION)
            .setInterpolator(OvershootInterpolator(1.4f))
            .setListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(MATCH_DURATION)
                        .setListener(null)
                        .withEndAction { onEnd?.invoke() }
                        .start()
                }
            })
            .start()
    }

    /**
     * 在 RecyclerView 网格中始终按正方形显示：高度跟随宽度。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val side = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(1)
        setMeasuredDimension(side, side)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        backRect.set(0, 0, w, h)
        backBitmap?.recycle()
        backBitmap = createBackBitmap(w, h)
    }

    /** 把牌背图案画成一张位图，避免每帧重画大量线条。 */
    private fun createBackBitmap(w: Int, h: Int): Bitmap? {
        if (w <= 0 || h <= 0) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val inset = w * 0.06f
        val border = w * 0.025f

        canvas.drawColor(backBgColor)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = backInnerColor
        canvas.drawRoundRect(
            inset + border, inset + border, w - inset - border, h - inset - border,
            w * 0.08f, w * 0.08f, paint
        )
        // 菱形网格纹理
        paint.color = backGridColor
        paint.strokeWidth = (w * 0.022f).coerceAtLeast(1f)
        val step = w / 5f
        var x = -h.toFloat()
        while (x < w) {
            canvas.drawLine(x, 0f, x + h, h.toFloat(), paint)
            canvas.drawLine(x, h.toFloat(), x + h, 0f, paint)
            x += step
        }
        // 中间“?”标记
        paint.color = backMarkColor
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = h * 0.18f
        canvas.drawText("?", w / 2f, h / 2f + h * 0.065f, paint)
        return bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 牌背纹理由位图绘制；圆角与描边来自背景 drawable
        if (!isFaceUp) {
            backBitmap?.let { canvas.drawBitmap(it, null, backRect, null) }
        } else {
            drawFace(canvas)
        }
    }

    /**
     * 在牌面上绘制“花色 + 点数”。
     * 字号按一行放得下、且不超出牌高度来计算，保证既不溢出也不留大片空白。
     */
    private fun drawFace(canvas: Canvas) {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val padding = (w * 0.10f).coerceAtLeast(1f)
        val availableWidth = w - 2 * padding
        val availableHeight = h - 2 * padding
        if (availableWidth <= 0f || availableHeight <= 0f) return

        textPaint.textSize = w.toFloat()
        textPaint.color = if (redSuit) suitRedColor else suitBlackColor

        val suitWidth = textPaint.measureText(suit)
        val rankWidth = textPaint.measureText(rank)
        var size = w * (availableWidth / (suitWidth + rankWidth + w * GAP_RATIO))

        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        if (textHeight > availableHeight) {
            size *= availableHeight / textHeight
        }
        if (size <= 0f) return

        textPaint.textSize = size
        val suitW = textPaint.measureText(suit)
        val rankW = textPaint.measureText(rank)
        val gap = size * GAP_RATIO
        val totalWidth = suitW + gap + rankW
        val startX = (w - totalWidth) / 2f
        val baseline = h / 2f - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f

        canvas.drawText(suit, startX, baseline, textPaint)
        canvas.drawText(rank, startX + suitW + gap, baseline, textPaint)
    }
}
