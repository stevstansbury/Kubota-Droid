package com.android.kubota.ui.equipment

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.kubota.R
import com.android.kubota.extensions.*
import com.android.kubota.ui.GaugeView
import com.kubota.service.domain.EquipmentUnit

private const val ENGINE_HOURS_FORMAT = "%.2f"

class MachineCardView: FrameLayout {

    private lateinit var imageView: ImageView
    private lateinit var nicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHoursTextView: TextView
    private lateinit var arrow: ImageView
    private lateinit var equipmentCheckBox: CheckBox
    private lateinit var ignitionIndicator: ImageView
    private lateinit var motionIndicator: ImageView
    private lateinit var warningIndicator: ImageView
    private lateinit var warningTextView: TextView
    private lateinit var machineCardView: View
    private lateinit var defLevel: GaugeView
    private lateinit var fuelLevel: GaugeView
    private lateinit var batteryIndicator: BatteryIndicatorView
    private lateinit var telematicsGroup: LinearLayout
    private lateinit var gaugesGroup: LinearLayout
    private lateinit var locationGroup: LinearLayout

    private val warningIconBitmap: Bitmap

    private val textPaint: TextPaint

    private lateinit var equipmentModel: EquipmentUnit
    private var editEnabled: Boolean = false
    private var isEquipmentSelected: Boolean = false

    private val defaultSize = resources.getDimensionPixelSize(R.dimen.gauge_view_default_size)

    private val normalBottomPadding = resources.getDimensionPixelSize(R.dimen.machine_card_bottom_padding)
    private val extendedBottomPadding = resources.getDimensionPixelSize(R.dimen.machine_card_extended_bottom_padding)

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        warningIconBitmap = BitmapFactory
            .decodeResource(
                context.resources,
                R.drawable.ic_equipment_warning
            )

        textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = context.resources.getDimension(R.dimen.warning_counter_text_size)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        LayoutInflater.from(context).inflate(R.layout.view_machine_card, this)

        imageView = findViewById(R.id.modelImage)
        nicknameTextView = findViewById(R.id.equipmentNickname)
        modelTextView = findViewById(R.id.modelName)
        serialNumberTextView = findViewById(R.id.equipmentPin)
        engineHoursTextView = findViewById(R.id.equipmentHours)
        arrow = findViewById(R.id.chevron)
        equipmentCheckBox = findViewById(R.id.checkbox)
        ignitionIndicator = findViewById(R.id.ignitionIndicator)
        motionIndicator = findViewById(R.id.motionIndicator)
        warningIndicator = findViewById(R.id.warningIndicator)
        warningTextView = findViewById(R.id.warningMessage)
        machineCardView = findViewById(R.id.machineCardView)
        defLevel = findViewById(R.id.defGauge)
        fuelLevel = findViewById(R.id.fuelGauge)
        batteryIndicator = findViewById(R.id.batteryMeter)
        telematicsGroup = findViewById(R.id.telematicsLayout)
        gaugesGroup = findViewById(R.id.gaugesLayout)
        locationGroup = findViewById(R.id.locationLayout)
    }

    fun setModel(equipment: EquipmentUnit) {
        equipmentModel = equipment
        updateModelViews()
    }

    fun setEditEnabled(enabled: Boolean) {
        editEnabled = enabled
        updateCABModeViews()
    }

    fun selectEquipment(select: Boolean) {
        isEquipmentSelected = select
        updateCABModeViews()
    }

    fun getSelectEquipment() = isEquipmentSelected

    private fun updateCABModeViews() {
        if (editEnabled){
            equipmentCheckBox.visibility = View.VISIBLE
            arrow.visibility = View.GONE
            equipmentCheckBox.isChecked = isEquipmentSelected
        } else {
            equipmentCheckBox.visibility = View.GONE
            arrow.visibility = View.VISIBLE
        }
    }

    private fun updateModelViews() {
        setBasicEquipmentInfo()
        setIndicatorInfo()

        when {
            equipmentModel.errorMessage.isNullOrBlank() -> warningTextView.visibility = View.GONE
            else -> {
                warningTextView.visibility = View.VISIBLE
                warningTextView.text = equipmentModel.errorMessage
            }
        }

        var showGaugesGroup = false
        defLevel.visibility =
            equipmentModel
                .defLevelPercent
                ?.let {
                    showGaugesGroup = true
                    defLevel.setPercent(it/100.0)
                    View.VISIBLE
                }
                ?: View.GONE

        fuelLevel.visibility =
            equipmentModel
                .fuelLevelPercent
                ?.let {
                    showGaugesGroup = true
                    fuelLevel.setPercent(it/100.0)
                    View.VISIBLE
                }
                ?: View.GONE

        batteryIndicator.visibility = equipmentModel
            .batteryVoltage
            ?.let {
                showGaugesGroup = true
                batteryIndicator.setVoltage(it)

                View.VISIBLE
            }
            ?: View.GONE

        gaugesGroup.visibility = if (showGaugesGroup) {
            View.VISIBLE
        } else {
            View.GONE
        }

        locationGroup.visibility = gaugesGroup.visibility

        telematicsGroup.visibility = if (showGaugesGroup || locationGroup.visibility == View.VISIBLE) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (areViewsVisible(gaugesGroup)) {
            var layoutParams: LinearLayout.LayoutParams
            if (areViewsVisible(batteryIndicator, fuelLevel, defLevel)) {
                // Resize the Layouts to take half screen
                layoutParams = (locationGroup.layoutParams as LinearLayout.LayoutParams)
                layoutParams.weight = 1.0f
                layoutParams.width = 0
                locationGroup.layoutParams = layoutParams

                layoutParams = (gaugesGroup.layoutParams as LinearLayout.LayoutParams)
                layoutParams.weight = 1.0f
                layoutParams.width = 0
                gaugesGroup.layoutParams = layoutParams
            } else {
                layoutParams = (gaugesGroup.layoutParams as LinearLayout.LayoutParams)
                layoutParams.weight = 0f

                var newParentWidth = 0
                for (idx in 0 until gaugesGroup.childCount) {
                    val childView = gaugesGroup.getChildAt(idx)

                    if (childView.visibility != View.VISIBLE) continue

                    val childLayoutParams = childView.layoutParams as ViewGroup.MarginLayoutParams
                    Log.e("JAIME", "childWidth: ${childLayoutParams.width} childMarginStart: ${childLayoutParams.marginStart} childMarginEnd ${childLayoutParams.marginEnd}")
                    Log.e("JAIME", "Before newParentWidth: ${newParentWidth}")
                    newParentWidth += childLayoutParams.width + childLayoutParams.marginStart + childLayoutParams.marginEnd
                    Log.e("JAIME", "After newParentWidth: ${newParentWidth}")
                }

                Log.e("JAIME", "Reset width to $newParentWidth")
                layoutParams.width = newParentWidth
                gaugesGroup.layoutParams = layoutParams
            }
        }
    }

    private fun setBasicEquipmentInfo() {
        if (equipmentModel.imageResId != 0) {
            imageView.setImageResource(equipmentModel.imageResId)
        } else {
            imageView.setImageResource(R.drawable.ic_construction_category_thumbnail)
        }

        when {
            equipmentModel.nickName.isNullOrBlank() -> nicknameTextView.text =
                equipmentModel.model
            else -> nicknameTextView.text = equipmentModel.nickName
        }

        modelTextView.text =
            resources
                .getString(
                    R.string.equipment_model_fmt,
                    equipmentModel.model
                )

        serialNumberTextView.text = resources.getString(R.string.equipment_pin_fmt, equipmentModel.serial)

        engineHoursTextView.text = ENGINE_HOURS_FORMAT.format(equipmentModel.engineHours)
    }

    private fun setIndicatorInfo() {
        if (equipmentModel.ignitionDrawableResId != 0) {
            ignitionIndicator.visibility = View.VISIBLE
            ignitionIndicator.setImageResource(equipmentModel.ignitionDrawableResId)
        } else {
            ignitionIndicator.visibility = View.INVISIBLE
        }

        when (equipmentModel.motionDrawableResId) {
            0 -> {
                motionIndicator.visibility = View.INVISIBLE
            }
            else -> {
                motionIndicator.setImageResource(equipmentModel.motionDrawableResId)
                motionIndicator.visibility = View.VISIBLE
            }
        }

        when (val warningIcon = generateWarningIcon(equipmentModel.numberOfWarnings)) {
            null -> warningIndicator.visibility = View.GONE
            else -> {
                warningIndicator.setImageBitmap(warningIcon)
                warningIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun areViewsVisible(vararg views: View): Boolean {
        var areVisible = true
        for (view in views) {
            areVisible = areVisible && view.visibility == View.VISIBLE

            if (!areVisible) return false
        }

        return true
    }

    private fun generateWarningIcon(numberOfWarnings: Int): Bitmap? {
        if (numberOfWarnings < 1) return null

        val warningsString = when  {
            numberOfWarnings > 9 -> resources.getString(R.string.warnings)
            else -> numberOfWarnings.toString()
        }

        val newIcon = warningIconBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(newIcon)
        val textWidth = canvas.width
        val staticLayout = StaticLayout(
            warningsString,
            textPaint,
            textWidth,
            Layout.Alignment.ALIGN_NORMAL,
            0f,
            0f,
            false
        )
        val bounds = Rect().apply {
            textPaint.getTextBounds(warningsString, 0, warningsString.length, this)
        }

        val x = (newIcon.width - bounds.width()) / 2f
        val y = (newIcon.height - bounds.height()) / 2.5f

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return newIcon
    }

    private fun convertToDP(sizeInDP: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDP, resources.displayMetrics)
    }
}
