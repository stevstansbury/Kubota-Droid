package com.android.kubota.ui.equipment

import android.content.Context
import android.graphics.*
import android.location.Geocoder
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.kubota.R
import com.android.kubota.extensions.*
import com.android.kubota.ui.GaugeView
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.outsideGeofence
import java.util.*
import kotlin.random.Random

private const val ENGINE_HOURS_FORMAT = "%.2f"

class MachineCardView: FrameLayout {

    private lateinit var editButton: ImageView
    private lateinit var imageView: ImageView
    private lateinit var nicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHoursTextView: TextView
    private lateinit var arrow: ImageView
    private lateinit var equipmentCheckBox: CheckBox
    private lateinit var space: View
    private lateinit var ignitionIndicator: ImageView
    private lateinit var motionIndicator: ImageView
    private lateinit var warningIndicator: ImageView
    private lateinit var warningTextView: TextView
    private lateinit var defLevel: GaugeView
    private lateinit var fuelLevel: GaugeView
    private lateinit var batteryIndicator: BatteryIndicatorView
    private lateinit var telematicsGroup: LinearLayout
    private lateinit var gaugesGroup: LinearLayout
    private lateinit var locationGroup: LinearLayout
    private lateinit var geofenceImageView: ImageView
    private lateinit var addressLabel: TextView

    private val warningIconBitmap: Bitmap

    private val textPaint: TextPaint

    private lateinit var equipmentModel: EquipmentUnit
    private var isEquipmentSelected: Boolean = false
    private var editClickListener: OnEditViewClicked? = null
    private var locationClickListener: OnLocationViewClicked? = null
    private val geocoder: Geocoder? by lazy { Geocoder(this.context, Locale.getDefault()) }

    private var viewType: ViewType

    enum class ViewType {
        List,
        CAB,
        Detail,
        Edit
    }

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

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MachineCardView)
        viewType = when (typedArray.getInt(R.styleable.MachineCardView_viewType, 0)) {
            1 -> ViewType.CAB
            2 -> ViewType.Detail
            3 -> ViewType.Edit
            else -> ViewType.List
        }
        typedArray.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        LayoutInflater.from(context).inflate(R.layout.view_machine_card, this)

        editButton = findViewById(R.id.editButton)
        imageView = findViewById(R.id.modelImage)
        nicknameTextView = findViewById(R.id.equipmentNickname)
        modelTextView = findViewById(R.id.modelName)
        serialNumberTextView = findViewById(R.id.equipmentPin)
        engineHoursTextView = findViewById(R.id.equipmentHours)
        arrow = findViewById(R.id.chevron)
        equipmentCheckBox = findViewById(R.id.checkbox)
        space = findViewById(R.id.space)
        ignitionIndicator = findViewById(R.id.ignitionIndicator)
        motionIndicator = findViewById(R.id.motionIndicator)
        warningIndicator = findViewById(R.id.warningIndicator)
        warningTextView = findViewById(R.id.warningMessage)
        defLevel = findViewById(R.id.defGauge)
        fuelLevel = findViewById(R.id.fuelGauge)
        batteryIndicator = findViewById(R.id.batteryMeter)
        telematicsGroup = findViewById(R.id.telematicsLayout)
        gaugesGroup = findViewById(R.id.gaugesLayout)
        locationGroup = findViewById(R.id.locationLayout)
        geofenceImageView = findViewById(R.id.geofenceImageView)
        addressLabel = findViewById(R.id.geoaddress)

        when (viewType) {
            ViewType.List -> enterListMode()
            ViewType.CAB -> enterCABMode(isEquipmentSelected)
            ViewType.Detail -> enterDetailMode()
        }

        locationGroup.setOnClickListener {
            locationClickListener?.onClick()
        }

        editButton.setOnClickListener {
            editClickListener?.onClick()
        }
    }

    fun setModel(equipment: EquipmentUnit, isSelected: Boolean = false) {
        equipmentModel = equipment
        updateModelViews()
        when (viewType) {
            ViewType.List -> enterListMode()
            ViewType.CAB -> enterCABMode(isSelected)
            ViewType.Detail -> enterDetailMode()
            ViewType.Edit -> enterEditMode()
        }
    }

    fun setOnLocationViewClicked(listener: OnLocationViewClicked) {
        locationClickListener = listener
    }

    fun setOnEditViewClicked(listener: OnEditViewClicked?) {
        editClickListener = listener
    }

    fun enterCABMode(isSelected: Boolean) {
        viewType = ViewType.CAB
        isEquipmentSelected = isSelected

        adjustViewTypeRelatedViews()
    }

    fun enterListMode() {
        viewType = ViewType.List

        adjustViewTypeRelatedViews()
    }

    fun enterDetailMode() {
        viewType = ViewType.Detail

        adjustViewTypeRelatedViews()
    }

    private fun enterEditMode() {
        viewType = ViewType.Edit

        adjustViewTypeRelatedViews()
    }

    private fun adjustViewTypeRelatedViews() {
        isEquipmentSelected = viewType == ViewType.CAB && isEquipmentSelected

        equipmentCheckBox.isChecked = isEquipmentSelected

        when (viewType) {
            ViewType.List -> {
                updateViewVisibility(View.GONE, equipmentCheckBox, space, editButton)
                updateViewVisibility(View.VISIBLE, arrow)
            }
            ViewType.CAB -> {
                updateViewVisibility(View.GONE, arrow, space, editButton)
                updateViewVisibility(View.VISIBLE, equipmentCheckBox)
            }
            ViewType.Detail -> {
                updateViewVisibility(View.GONE, equipmentCheckBox, arrow, space)
                updateViewVisibility(View.VISIBLE, editButton)
            }
            ViewType.Edit -> {
                updateViewVisibility(View.GONE, equipmentCheckBox, arrow, editButton)
                updateViewVisibility(View.VISIBLE, space)
            }
        }
    }

    fun getSelectEquipment() = isEquipmentSelected

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
                .telematics?.defRemainingPercent
                ?.let {
                    showGaugesGroup = true
                    defLevel.setPercent(it/100.0)
                    View.VISIBLE
                }
                ?: View.GONE

        fuelLevel.visibility =
            equipmentModel
                .telematics?.fuelRemainingPercent
                ?.let {
                    showGaugesGroup = true
                    fuelLevel.setPercent(it/100.0)
                    View.VISIBLE
                }
                ?: View.GONE

        batteryIndicator.visibility = equipmentModel
            .telematics?.extPowerVolts
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
                    newParentWidth += childLayoutParams.width + childLayoutParams.marginStart + childLayoutParams.marginEnd
                }

                layoutParams.width = newParentWidth
                gaugesGroup.layoutParams = layoutParams
            }
        }

        Promise.value(this.equipmentModel.telematics?.location)
                .map(on = DispatchExecutor.global) { coordinate ->
                    coordinate?.let { this.geocoder?.getFromLocation(it.latitude, it.longitude, 1)?.firstOrNull() }
                }
                .done { geoAddress ->
                    if (geoAddress != null) {
                        val locality = geoAddress.locality ?: ""
                        val state = geoAddress.adminArea ?: ""
                        val number = geoAddress.subThoroughfare ?: ""
                        this.addressLabel.text = geoAddress.thoroughfare?.let {
                            "$number $it\n$locality, $state"
                        } ?: "$locality $state"

                        val outsideGeofence = this.equipmentModel.telematics?.outsideGeofence ?: false
                        this.geofenceImageView.setImageResource(if (outsideGeofence) R.drawable.ic_outside_geofence else R.drawable.ic_inside_geofence)
                    } else {
                        // FIXME: Need unknown geofence icon
                        this.addressLabel.setText(R.string.location_unavailable)
                    }
                }
                .catch {
                    // FIXME: Need unknown geofence icon
                    this.addressLabel.setText(R.string.location_unavailable)
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

        serialNumberTextView.text =
            if (equipmentModel.pin != null)
                resources.getString(R.string.equipment_pin_fmt, equipmentModel.pinOrSerial)
            else
                resources.getString(R.string.equipment_serial_fmt, equipmentModel.pinOrSerial)

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

    private fun updateViewVisibility(newVisibility: Int, vararg views: View) {
        views.forEach { it.visibility = newVisibility }
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

    interface OnLocationViewClicked {
        fun onClick()
    }

    interface OnEditViewClicked {
        fun onClick()
    }
}
