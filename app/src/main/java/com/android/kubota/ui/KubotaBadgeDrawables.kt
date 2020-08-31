package com.android.kubota.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.android.kubota.R


private const val DEFAULT_EXCEED_MAX_BADGE_NUMBER_SUFFIX = "+"

class KubotaBadgeDrawables(
    private val context: Context,
    @ColorRes bkgColorResId: Int = R.color.notification_tab_unread_counter_color,
    @DimenRes sizeDimeResId: Int = R.dimen.notification_unread_counter_size
) : Drawable() {

    @ColorInt
    var backgroundColor: Int = ResourcesCompat.getColor(context.resources, bkgColorResId, null)
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

    private val intrinsicSize = context.resources.getDimensionPixelSize(sizeDimeResId)
    private val size = context.resources.getDimension(sizeDimeResId)
    private val maxBadgeNumber = 9
    private val textBounds = Rect()

    private val textPaint = TextPaint().apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.notification_tab_unread_counter_text_color)
        textSize = context.resources.getDimensionPixelSize(R.dimen.notification_tab_unread_counter_text_size).toFloat()
    }

    private val badgePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    init {
        // We do this because setValue is not called when the backgroundColor property is initialized
        badgePaint.color = backgroundColor
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

        val width = size
        val height = size
        val radius = Math.max(width, height) / 2
        val centerX = width / 2
        val centerY = height / 2
        canvas.drawCircle(centerX, centerY, radius, badgePaint)
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

    override fun getIntrinsicHeight() = intrinsicSize

    override fun getIntrinsicWidth() = intrinsicSize

    private fun hasNumber() = unreadCounter > 0
}