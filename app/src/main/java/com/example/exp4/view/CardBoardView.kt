package com.example.exp4.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

/**
 * 游戏界面（View）：把多张 [CardView] 排成规整的网格。
 *
 * 网格保持正方形比例并居中显示，行列间距自适应，保证界面整齐美观。
 */
class CardBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    /** 每行显示几张牌。 */
    var columns: Int = 3
        set(value) {
            field = value.coerceAtLeast(1)
            requestLayout()
        }

    /** 每列显示几张牌。 */
    var rows: Int = 4
        set(value) {
            field = value.coerceAtLeast(1)
            requestLayout()
        }

    /** 牌与牌之间的间距比例（相对于牌的宽度）。 */
    var spacingRatio: Float = 0.10f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)

        val spacing = (availableWidth * spacingRatio / columns).toInt().coerceAtLeast(1)
        var cardWidth = (availableWidth - (columns - 1) * spacing) / columns
        var cardHeight = (availableHeight - (rows - 1) * spacing) / rows

        // 保持每张牌为正方形，取较小的边长，避免牌被拉长或压扁
        val side = minOf(cardWidth, cardHeight).coerceAtLeast(1)
        cardWidth = side
        cardHeight = side

        val boardWidth = columns * cardWidth + (columns - 1) * spacing
        val boardHeight = rows * cardHeight + (rows - 1) * spacing

        // 居中显示
        val left = ((availableWidth - boardWidth) / 2).coerceAtLeast(0)
        val top = ((availableHeight - boardHeight) / 2).coerceAtLeast(0)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(cardWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cardHeight, MeasureSpec.EXACTLY)
            )
            val row = i / columns
            val col = i % columns
            child.layout(
                left + col * (cardWidth + spacing),
                top + row * (cardHeight + spacing),
                left + col * (cardWidth + spacing) + cardWidth,
                top + row * (cardHeight + spacing) + cardHeight
            )
        }

        setMeasuredDimension(availableWidth, availableHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 在 onMeasure 中已完成摆放
    }
}
