package com.aricneto.twistytimer.layout

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.aricneto.twistify.R
import kotlin.math.min
import androidx.core.content.withStyledAttributes

class RippleBackgroundView : FrameLayout {
    private var rippleColor = 0
    private var rippleStrokeWidth = 0f
    private var rippleRadius = 0f
    private var rippleDurationTime = 0
    private var rippleAmount = 0
    private var rippleDelay = 0
    private var rippleScale = 0f
    private var rippleType = 0

    private var paint: Paint? = null
    var isRippleAnimationRunning: Boolean = false
        private set
    private var animatorSet: AnimatorSet? = null
    private val rippleViewList: ArrayList<RippleView> = ArrayList<RippleView>()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (isInEditMode) return

        context.withStyledAttributes(attrs, R.styleable.RippleBackgroundView) {
            rippleColor = getColor(
                R.styleable.RippleBackgroundView_rb_color, resources.getColor(
                    R.color.md_blue_A700
                )
            )
            rippleStrokeWidth = getDimension(
                R.styleable.RippleBackgroundView_rb_strokeWidth, resources.getDimension(
                    R.dimen.rippleStrokeWidth
                )
            )
            rippleRadius = getDimension(
                R.styleable.RippleBackgroundView_rb_radius, resources.getDimension(
                    R.dimen.rippleRadius
                )
            )
            rippleDurationTime = getInt(R.styleable.RippleBackgroundView_rb_duration, 3000)
            rippleAmount = getInt(R.styleable.RippleBackgroundView_rb_rippleAmount, 6)
            rippleScale = getFloat(R.styleable.RippleBackgroundView_rb_scale, 6.0f)
            rippleType = getInt(R.styleable.RippleBackgroundView_rb_type, 0)
        }

        rippleDelay = rippleDurationTime / rippleAmount

        paint = Paint()
        paint!!.isAntiAlias = true
        if (rippleType == 0) {
            rippleStrokeWidth = 0f
            paint!!.style = Paint.Style.FILL
        } else {
            paint!!.style = Paint.Style.STROKE
        }
        paint!!.color = rippleColor

        val rippleParams = LayoutParams(
            (2 * (rippleRadius + rippleStrokeWidth)).toInt(),
            (2 * (rippleRadius + rippleStrokeWidth)).toInt()
        )
        rippleParams.gravity = Gravity.CENTER

        animatorSet = AnimatorSet()
        animatorSet!!.interpolator = AccelerateDecelerateInterpolator()
        val animators = ArrayList<Animator?>()

        for (i in 0..<rippleAmount) {
            val rippleView = RippleView(getContext())
            addView(rippleView, rippleParams)
            rippleViewList.add(rippleView)
            val scaleXAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleX", 1.0f, rippleScale)
            scaleXAnimator.repeatCount = ObjectAnimator.INFINITE
            scaleXAnimator.repeatMode = ObjectAnimator.RESTART
            scaleXAnimator.startDelay = (i * rippleDelay).toLong()
            scaleXAnimator.duration = rippleDurationTime.toLong()
            animators.add(scaleXAnimator)

            val scaleYAnimator = ObjectAnimator.ofFloat(rippleView, "ScaleY", 1.0f, rippleScale)
            scaleYAnimator.repeatCount = ObjectAnimator.INFINITE
            scaleYAnimator.repeatMode = ObjectAnimator.RESTART
            scaleYAnimator.startDelay = (i * rippleDelay).toLong()
            scaleYAnimator.duration = rippleDurationTime.toLong()
            animators.add(scaleYAnimator)

            val alphaAnimator = ObjectAnimator.ofFloat(rippleView, "Alpha", 1.0f, 0f)
            alphaAnimator.repeatCount = ObjectAnimator.INFINITE
            alphaAnimator.repeatMode = ObjectAnimator.RESTART
            alphaAnimator.startDelay = (i * rippleDelay).toLong()
            alphaAnimator.duration = rippleDurationTime.toLong()
            animators.add(alphaAnimator)
        }

        animatorSet!!.playTogether(animators)
    }

    private inner class RippleView(context: Context?) : View(context) {
        init {
            this.visibility = INVISIBLE
        }

        override fun onDraw(canvas: Canvas) {
            val radius = (min(getWidth(), getHeight())) / 2
            canvas.drawCircle(
                radius.toFloat(),
                radius.toFloat(),
                radius - rippleStrokeWidth,
                paint!!
            )
        }
    }

    fun startRippleAnimation() {
        if (!this.isRippleAnimationRunning) {
            for (rippleView in rippleViewList) {
                rippleView.visibility = VISIBLE
            }
            animatorSet!!.start()
            this.isRippleAnimationRunning = true
        }
    }

    fun stopRippleAnimation() {
        if (this.isRippleAnimationRunning) {
            animatorSet!!.end()
            this.isRippleAnimationRunning = false
            for (rippleView in rippleViewList) {
                rippleView.visibility = INVISIBLE
            }
        }
    }
}
