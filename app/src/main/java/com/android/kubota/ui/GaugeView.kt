package com.android.kubota.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.android.kubota.R


class GaugeView : View {

    private val rectF = RectF()

    private val percentStringFmt = resources.getString(R.string.gauge_view_percent_fmt)
    private val defaultSize = resources.getDimensionPixelSize(R.dimen.gauge_view_default_size)

    private val percentPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val outlinePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        textSize = context.resources.getDimension(R.dimen.gauge_view_text_size)
        textAlign = Paint.Align.CENTER
    }

    private val textBounds = Rect()
    
    private var percent = 0.0f
    private val strokeWidth = resources.getDimension(R.dimen.gauge_view_stroke_width)

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GaugeView)
        val temp = typedArray.getFloat(R.styleable.GaugeView_percent, 0.0f)
        setPercent(temp)
        typedArray.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    fun setPercent(value: Float) {
        percent = value
        determineGaugeColor()
    }

    private fun determineGaugeColor() {
        if (percent > 0.39f) {
            percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_green_meter)
            outlinePaint.color = ContextCompat.getColor(context, R.color.gauge_view_green_outline)
        } else {
            percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_yellow_meter)
            outlinePaint.color = ContextCompat.getColor(context, R.color.gauge_view_yellow_outline)
        }
        textPaint.color = percentPaint.color

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Determine Height
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val height = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            Math.min(defaultSize, heightSize)
        }
        val hSizeSpecs = MeasureSpec.makeMeasureSpec(height, heightMode)

        // Determine Width
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val width = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            Math.min(defaultSize, widthSize)
        }
        val wSizeSpecs = MeasureSpec.makeMeasureSpec(width, widthMode)

        super.onMeasure(wSizeSpecs, hSizeSpecs)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            outlinePaint.strokeWidth = strokeWidth
            percentPaint.strokeWidth = strokeWidth

            val verticalPadding = paddingTop + paddingBottom
            val adjustedHeight = measuredHeight - verticalPadding
            val verticalOffset = adjustedHeight * 0.25f

            rectF.right = measuredWidth - strokeWidth - paddingRight
            rectF.bottom = adjustedHeight + verticalOffset - paddingBottom
            rectF.left = paddingLeft + strokeWidth
            rectF.top = if (verticalOffset > strokeWidth) paddingTop + verticalOffset else paddingTop + strokeWidth

            textBounds.bottom = bottom - paddingBottom
            textBounds.left = (paddingLeft + strokeWidth).toInt()
            textBounds.right = (right - paddingRight - strokeWidth).toInt()
            textBounds.top = (paddingTop + strokeWidth).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(rectF, 180f, 180f, false, outlinePaint)
        val meterAngle =  percent * 180f
        canvas.drawArc(rectF, 180f, meterAngle, false, percentPaint)

        val temp = (percent * 100).toInt()
        val percentStr = String.format(percentStringFmt, "$temp", "%")
        textPaint.getTextBounds(percentStr, 0, percentStr.length, textBounds)
        val fontMetrics = textPaint.fontMetrics
        val yBaseline = ((rectF.bottom - rectF.top)/2f) - fontMetrics.ascent
        val xBaseline = (rectF.right - rectF.left)/2f + (textBounds.right - textBounds.left)/percentStr.length

        canvas.drawText(percentStr, xBaseline, yBaseline, textPaint)
    }
}