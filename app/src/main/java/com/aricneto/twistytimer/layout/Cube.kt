package com.aricneto.twistytimer.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.toColorInt
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.AlgUtils

class Cube : View {
    private var mCubeState: String? = null

    private var mCubePaint: Paint = Paint()
    private var mStickerPaint: Paint = Paint()

    private var mStickerRect: RectF = RectF()
    private var mCubeRect: RectF = RectF()

    private var mPadding = 0
    private var mStickerSize = 0f
    private var mCubeCornerRadius = 0f
    private var mStickerCornerRadius = 0f

    constructor(context: Context?) : super(context, null) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs, 0) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        initPaints()
        initRects()

        if (attrs == null) return

        context.withStyledAttributes(attrs, R.styleable.Cube) {
            mCubeCornerRadius =
                getDimensionPixelSize(R.styleable.Cube_cube_corner_radius, 8).toFloat()
            mStickerCornerRadius =
                getDimensionPixelSize(R.styleable.Cube_cube_sticker_corner_radius, 8).toFloat()
        }
    }

    private fun initPaints() {
        mCubePaint.style = Paint.Style.FILL
        mCubePaint.color = "#2E2E2E".toColorInt()
        mCubePaint.isAntiAlias = true

        mStickerPaint.style = Paint.Style.FILL
        mStickerPaint.isAntiAlias = true
    }

    private fun initRects() {
        mCubeRect = RectF()
        mStickerRect = RectF()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val state = mCubeState ?: return

        val width = getWidth()

        // padding is 3% of the cube's size
        mPadding = (width * 0.04f).toInt()

        // sticker size. in a 3x3, there'll be 5 stickers (3 for the face, 2 for the sides)
        // in each line.
        mStickerSize = (width - (mPadding * 6)) / (3 + 0.75f)
        // size to subtract from outermost stickers
        // you MUST change mStickerSize to divide by the correct amount
        // i.e. if sizeSubtract = mStickerSize / 2, there'll be 3 whole stickers and 2 half-stickers
        // which equals 4 stickers. Likewise, if you were to divide by 1.6, there would be 3.75 stickers
        val sizeSubtract = (mStickerSize / 1.6f)

        mCubeRect.set(
            0f, 0f,
            (mPadding * 6) + (mStickerSize * 5) - (sizeSubtract * 2),
            (mPadding * 6) + (mStickerSize * 5) - (sizeSubtract * 2)
        )

        // draw cube background
        canvas.drawRoundRect(mCubeRect, mCubeCornerRadius, mCubeCornerRadius, mCubePaint)

        // Draw the cube
        // The edge conditions are used to draw half-stickers in the borders only
        // This code is rather complicated to explain
        // Basically, for every line, we create a rect at the beginning. We then draw that rect
        // and use its properties to calculate where the next sticker should be.
        for (i in 0..4) {
            mStickerRect.set(
                mPadding.toFloat(),
                mPadding + ((mStickerSize + mPadding) * i) - sizeSubtract,
                mPadding + mStickerSize,
                mPadding + mStickerSize + ((mStickerSize + mPadding) * i) - sizeSubtract
            )

            // top outer border
            if (i == 0) {
                mStickerRect.set(
                    mStickerRect.left,
                    mPadding.toFloat(),
                    mStickerRect.right,
                    mStickerRect.bottom
                )
            }

            // bottom outer border
            if (i == 4) {
                mStickerRect.set(
                    mStickerRect.left,
                    mStickerRect.top,
                    mStickerRect.right,
                    mStickerRect.bottom - sizeSubtract
                )
            }

            for (j in 0..4) {
                // left outer border

                if (j == 0) {
                    mStickerRect.set(
                        mPadding.toFloat(),  //mStickerRect.left + sizeSubtract,
                        mStickerRect.top,
                        mStickerRect.right - sizeSubtract,
                        mStickerRect.bottom
                    )
                }

                // ignore the four corners
                // TODO: Error check if string is of correct size/format!
                if (!((i == 0 || i == 4) && (j == 0 || j == 4))) {
                    mStickerPaint.color = AlgUtils.getColorFromStateIndex(
                        state,
                        (5 * i) + j
                    )
                    canvas.drawRoundRect(
                        mStickerRect,
                        mStickerCornerRadius,
                        mStickerCornerRadius,
                        mStickerPaint
                    )
                }
                mStickerRect.set(
                    mStickerRect.right + mPadding,
                    mStickerRect.top,
                    if (j != 3) mStickerRect.right + mPadding + mStickerSize else mStickerRect.right + mPadding + mStickerSize - sizeSubtract,  // right outer border
                    mStickerRect.bottom
                )
            }
        }
    }

    var cubeState: String?
        get() = mCubeState
        set(cubeState) {
            this.mCubeState = cubeState
            invalidate()
        }
}
