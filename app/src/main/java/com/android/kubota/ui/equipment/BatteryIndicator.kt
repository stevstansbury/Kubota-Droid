package com.android.kubota.ui.equipment

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.kubota.R

class BatteryIndicatorView: FrameLayout {

    private val voltageFormat: String = "%.1f"
    private val batteryImage: Drawable
    private var voltage: Double = 0.0

    private lateinit var voltageTextView: TextView
    private lateinit var batteryImageView: ImageView

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        batteryImage = context.resources.getDrawable(R.drawable.ic_battery_green, null)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BatteryIndicatorView)
        voltage = typedArray.getFloat(R.styleable.BatteryIndicatorView_voltage, 0.0f).toDouble()

        typedArray.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        LayoutInflater.from(context).inflate(R.layout.view_battery_indicator, this)
        voltageTextView = findViewById(R.id.batteryLabel)
        batteryImageView = findViewById(R.id.batteryImage)
    }

    fun setVoltage(double: Double) {
        voltage = double
        update()
    }

    private fun update() {
        voltageFormat.format(voltage)
        voltageTextView.text = "${voltageFormat.format(voltage)}v"
        if (voltage >= 12.0) {
            batteryImageView.setImageResource(R.drawable.ic_battery_green)
        } else if (voltage >= 11.5) {
            batteryImageView.setImageResource(R.drawable.ic_battery_brown)
        } else {
            batteryImageView.setImageResource(R.drawable.ic_battery_red)
        }
    }
}