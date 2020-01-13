package com.android.kubota.utility

object AnimationUtils {

    fun lerp(startValue: Float, endValue: Float, fraction: Float): Float {
        return startValue + fraction * (endValue - startValue)
    }

    fun lerp(startValue: Int, endValue: Int, fraction: Float): Int {
        return startValue + Math.round(fraction * (endValue - startValue).toFloat())
    }
}