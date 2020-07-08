package com.android.kubota.ui

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.android.kubota.R

private enum class GaugeType {
    DEF, FUEL
}

class GaugeView : View {

    private val reusableRect = Rect()
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
        color = ContextCompat.getColor(context, R.color.gauge_view_meter_outline)
    }

    private val labelTextPaint = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        textSize = context.resources.getDimension(R.dimen.gauge_view_label_size)
        color = ContextCompat.getColor(context, R.color.gauge_view_label_text_color)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val labelTextBounds = Rect()

    private val percentTextPaint = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        textSize = context.resources.getDimension(R.dimen.gauge_view_percent_size)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val percentTextBounds = Rect()
    
    private var percent = 0.0
    private var label = ""
    private val strokeWidth = resources.getDimension(R.dimen.gauge_view_stroke_width)
    private var gaugeType = GaugeType.DEF

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GaugeView)
        val temp = typedArray.getFloat(R.styleable.GaugeView_percent, 0.0f).toDouble()
        setPercent(temp)
        label = typedArray.getString(R.styleable.GaugeView_text) ?: label
        gaugeType = GaugeType.values()[typedArray.getInt(R.styleable.GaugeView_gaugeType, 0)]
        typedArray.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    fun setPercent(value: Double) {
        percent = value
        determineGaugeColor()
    }

    private fun determineGaugeColor() {
        if (gaugeType == GaugeType.FUEL) {
            if (percent > .26f) {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_green_meter)
            } else if (percent >= .11f) {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_yellow_meter)
            } else {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_red_meter)
            }
        } else {
            if (percent > .36f) {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_green_meter)
            } else if (percent >= .16f) {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_yellow_meter)
            } else {
                percentPaint.color = ContextCompat.getColor(context, R.color.gauge_view_red_meter)
            }
        }

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

            labelTextPaint.getTextBounds(label, 0, label.length, reusableRect)

            labelTextBounds.top = paddingTop
            labelTextBounds.right = right - paddingRight
            labelTextBounds.left = paddingLeft
            labelTextBounds.bottom = paddingTop + reusableRect.height()

            val bottomMargin = convertToDP(1.0f)

            rectF.bottom = adjustedHeight + verticalOffset - paddingBottom
            val tempBottom = adjustedHeight + verticalOffset - paddingBottom
            val tempLeft = paddingLeft + strokeWidth
            val tempRight = measuredWidth - strokeWidth - paddingRight
            val tempTop = labelTextBounds.bottom + strokeWidth + labelTextPaint.fontMetrics.bottom + bottomMargin

            //left <= right and top <= bottom)
            val w = tempRight - tempLeft
            val h = tempBottom - tempTop

            if (h > w) {
                val offset = (h - w)/2
                rectF.top = tempTop + offset
                rectF.left = tempLeft
                rectF.right = tempRight
                rectF.bottom = tempBottom - offset
            } else if (w > h) {
                rectF.top = labelTextBounds.bottom + strokeWidth + labelTextPaint.fontMetrics.bottom + bottomMargin
                val offset = (w - h)/2
                rectF.left = tempLeft + offset
                rectF.right = tempRight - offset
                rectF.bottom = tempBottom
            } else {
                rectF.top = tempTop
                rectF.left = tempLeft
                rectF.right = tempRight
                rectF.bottom = tempBottom
            }

            percentTextBounds.bottom = bottom - paddingBottom
            percentTextBounds.left = (paddingLeft + strokeWidth).toInt()
            percentTextBounds.right = (right - paddingRight - strokeWidth).toInt()
            percentTextBounds.top = (paddingTop + strokeWidth).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var fontMetrics = labelTextPaint.fontMetrics
        var yBaseline = 0.0f
        var xBaseline = 0.0f

        if (label.isNotEmpty()) {
            labelTextPaint.getTextBounds(label, 0, label.length, labelTextBounds)
            yBaseline = ((labelTextBounds.bottom - labelTextBounds.top) / 2f) - fontMetrics.ascent
            xBaseline = (rectF.right - rectF.left) / 2f + (labelTextBounds.right - labelTextBounds.left) / label.length
            canvas.drawText(label, xBaseline, yBaseline, labelTextPaint)
        }

        canvas.drawArc(rectF, 180f, 180f, false, outlinePaint)
        val meterAngle =  percent.toFloat() * 180f
        canvas.drawArc(rectF, 180f, meterAngle, false, percentPaint)

        val temp = (percent * 100).toInt()
        val percentStr = String.format(percentStringFmt, "$temp", "%")
        percentTextPaint.getTextBounds(percentStr, 0, percentStr.length, percentTextBounds)
        fontMetrics = percentTextPaint.fontMetrics

        if (label.isNotEmpty()) {
            yBaseline = rectF.bottom/2f - fontMetrics.ascent + 5f
        } else {
            yBaseline = rectF.bottom/2f - fontMetrics.ascent
        }
        xBaseline = (rectF.right - rectF.left)/2f + (percentTextBounds.right - percentTextBounds.left)/percentStr.length + 5f

        canvas.drawText(percentStr, xBaseline, yBaseline, percentTextPaint)
    }

    private fun convertToDP(sizeInDP: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDP, resources.displayMetrics)
    }
}