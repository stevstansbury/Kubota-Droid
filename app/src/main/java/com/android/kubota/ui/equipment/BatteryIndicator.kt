package com.android.kubota.ui.equipment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import com.android.kubota.R
import com.kubota.service.domain.TelematicStatus
import com.kubota.service.domain.voltageStatus

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
        voltageTextView.text = "${voltageFormat.format(Math.floor(voltage * 10.0) / 10.0)}v"
        when (voltageStatus(voltage)) {
            TelematicStatus.Normal -> {
                batteryImageView.setImageResource(R.drawable.ic_battery_green)
                voltageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.battery_indicator_green, null))
            }
            TelematicStatus.Warning -> {
                batteryImageView.setImageResource(R.drawable.ic_battery_brown)
                voltageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.battery_indicator_brown, null))
            }
            TelematicStatus.Critical -> {
                batteryImageView.setImageResource(R.drawable.ic_battery_red)
                voltageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.battery_indicator_red, null))
            }
        }
    }
}