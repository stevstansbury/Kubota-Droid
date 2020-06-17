package com.android.kubota.ui

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.kubota.R


class DottedProgressBar: View {

    private val drawable: LayerDrawable

    constructor(context: Context): this(context, null)

    constructor(
        context: Context,
        attrs: AttributeSet?
    ): this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ): this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ): super(context, attrs, defStyleAttr, defStyleRes) {
        drawable = context.getDrawable(R.drawable.dotted_progress_bar) as LayerDrawable
        background = drawable
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        //Animation called when attaching to the window, i.e to your screen
        startAnimation()
    }

    private fun startAnimation() {
        val dot1 = drawable.findDrawableByLayerId(R.id.dot1) as GradientDrawable
        val dot2 = drawable.findDrawableByLayerId(R.id.dot2) as GradientDrawable
        val dot3 = drawable.findDrawableByLayerId(R.id.dot3) as GradientDrawable
        val dot4 = drawable.findDrawableByLayerId(R.id.dot4) as GradientDrawable

        val anim1 = createAnim(dot1, 0)
        val anim2 = createAnim(dot2, 300)
        val anim3 = createAnim(dot3, 600)
        val anim4 = createAnim(dot4, 900)
        
        val set = AnimatorSet()
        set.playTogether(anim1, anim2, anim3, anim4)
        set.start()
    }

    private fun createAnim(drawable: GradientDrawable, delay: Long): ValueAnimator {
        val anim = ObjectAnimator.ofObject(
            drawable,
            "tint",
            ArgbEvaluator(),
            Color.parseColor("#215794"),
            Color.parseColor("#d9e3f2")
        )
        anim.duration = 1000
        anim.repeatCount = ValueAnimator.INFINITE
        anim.startDelay = delay

        return anim
    }
}