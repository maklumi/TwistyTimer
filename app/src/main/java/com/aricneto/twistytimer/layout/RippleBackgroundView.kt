package com.aricneto.twistytimer.layout

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.aricneto.twistify.R
import com.google.android.material.color.MaterialColors

class RippleBackgroundView : FrameLayout {
    private var rippleColor = 0
    private var rippleStrokeWidth = 0f
    private var rippleRadius = 0f
    private var rippleDurationTime = 0
    private var rippleAmount = 0
    private var rippleScale = 0f
    private var rippleType = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val interpolator = AccelerateDecelerateInterpolator()
    
    var isRippleAnimationRunning: Boolean = false
        private set
    private var animator: ValueAnimator? = null

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
        setWillNotDraw(false)

        context.withStyledAttributes(attrs, R.styleable.RippleBackgroundView) {
            rippleColor = getColor(
                R.styleable.RippleBackgroundView_rb_color,
                MaterialColors.getColor(context, R.attr.colorTertiary, Color.BLUE)
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

        paint.color = rippleColor
        paint.strokeWidth = rippleStrokeWidth
        paint.style = if (rippleType == 0) Paint.Style.FILL else Paint.Style.STROKE

        animator = ValueAnimator.ofFloat(0f, 1.0f).apply {
            duration = rippleDurationTime.toLong()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { postInvalidateOnAnimation() }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRippleAnimationRunning) return

        val centerX = width / 2f
        val centerY = height / 2f
        val progress = animator?.animatedValue as? Float ?: 0f

        for (i in 0 until rippleAmount) {
            // Stagger each ripple correctly
            var rippleProgress = progress - (i.toFloat() / rippleAmount)
            if (rippleProgress < 0) rippleProgress += 1.0f
            
            // Apply the smooth curve to the staggered progress
            val smoothProgress = interpolator.getInterpolation(rippleProgress)
            
            // Calculate transparency and scale based on the smooth progress
            paint.alpha = ((1f - smoothProgress) * 255).toInt()
            val currentRadius = rippleRadius * (1f + smoothProgress * (rippleScale - 1f))
            
            canvas.drawCircle(centerX, centerY, currentRadius, paint)
        }
    }

    fun startRippleAnimation() {
        if (!isRippleAnimationRunning) {
            isRippleAnimationRunning = true
            animator?.start()
        }
    }

    fun stopRippleAnimation() {
        if (isRippleAnimationRunning) {
            isRippleAnimationRunning = false
            animator?.cancel()
            postInvalidateOnAnimation()
        }
    }
}
