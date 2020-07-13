package com.android.kubota.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.android.kubota.R


private const val DEFAULT_EXCEED_MAX_BADGE_NUMBER_SUFFIX = "+"

class KubotaBadgeDrawables(private val context: Context): Drawable() {

    @ColorInt
    var backgroundColor: Int = Color.parseColor("#FFFFFF")
        set(value) {
            field = value
            badgePaint.color = value
            invalidateSelf()
        }

    var unreadCounter: Int = 0
        set(value) {
            field = value
            invalidateSelf()
        }


    private val maxBadgeNumber = 9
    private val textBounds = Rect()

    private val textPaint = TextPaint().apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    private val badgePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun setTextTypeFace(typeface: Typeface) {
        textPaint.typeface = typeface
    }

    fun setTextSize(@DimenRes resId: Int) {
        textPaint.textSize = context.resources.getDimensionPixelSize(resId).toFloat()
    }

    fun setTextColor(@ColorRes resId: Int) {
        textPaint.color = ContextCompat.getColor(context, resId)
    }

    override fun draw(canvas: Canvas) {
        if (!hasNumber()) return

        val bounds = bounds
        val width = (bounds.right - bounds.left).toFloat()
        val height = (bounds.bottom - bounds.top).toFloat()
        val radius = Math.max(width, height) / 2 / 2
        val centerX = width / 2
        val centerY = height / 2
        if (unreadCounter <= 9) {
            canvas.drawCircle(centerX, centerY, radius + 7.5f, badgePaint)
        } else {
            canvas.drawCircle(centerX, centerY, radius + 8.5f, badgePaint)
        }
        val unreadText = getBadgeText()
        textPaint.getTextBounds(unreadText, 0, unreadText.length, textBounds)
        val textHeight: Float = (textBounds.bottom - textBounds.top).toFloat()
        val textY = centerY + textHeight / 2f
        canvas.drawText(unreadText, centerX, textY, textPaint)
    }

    fun getNumber() = if (!hasNumber()) 0 else unreadCounter

    private fun getBadgeText(): String {
        // If number exceeds max count, show badgeMaxCount+ instead of the number.
        return if (getNumber() <= maxBadgeNumber) {
            getNumber().toString()
        } else {
            context.getString(
                R.string.exceed_max_badge_number_suffix,
                maxBadgeNumber,
                DEFAULT_EXCEED_MAX_BADGE_NUMBER_SUFFIX
            )
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity() = PixelFormat.UNKNOWN

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    fun hasNumber() = unreadCounter > 0
}