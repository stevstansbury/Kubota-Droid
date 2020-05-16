package com.android.kubota.ui.equipment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.Observer
import androidx.fragment.app.DialogFragment
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.hasManual
import com.android.kubota.ui.*
import com.android.kubota.utility.AccountPrefs
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentUnit
import java.util.*


class EquipmentDetailFragment : BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var modelImageView: ImageView
    private lateinit var equipmentNicknameTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var serialNumberTextView: TextView
    private lateinit var engineHoursTextView: TextView
    private lateinit var engineHoursButton: View
    private lateinit var hoursToNextServiceButton: View
    private lateinit var manualsButton: View
    private lateinit var guidesButton: View
    private lateinit var editSerialNumber: ImageView
    private lateinit var faultCodeItem: View

    private lateinit var telematicsViewsGroup: Group
    private lateinit var locationStatusBarrier: View
    private lateinit var statusLabel: View
    private lateinit var statusTextView: TextView
    private lateinit var locationLabel: View
    private lateinit var locationTextView: TextView
    private lateinit var batteryLabel: View
    private lateinit var gaugesBarrier: View
    private lateinit var batteryGaugeView: GaugeView
    private lateinit var fuelLabel: View
    private lateinit var fuelGaugeView: GaugeView
    private lateinit var defLabel: View
    private lateinit var defGaugeView: GaugeView
    private lateinit var telematicsButton: View
    private lateinit var telematicsDivider: View

    companion object {
        private const val SERIAL_NUMBER_EDIT_REQUEST_CODE = 5
        const val EQUIPMENT_KEY = "equipment"

        fun createInstance(equipmentId: UUID): EquipmentDetailFragment {
            return EquipmentDetailFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SERIAL_NUMBER_EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // TODO: This case is no longer valid
            return
        }

        return super.onActivityResult(requestCode, resultCode, data)
    }

    override fun initUi(view: View) {
        modelImageView = view.findViewById(R.id.equipmentImage)
        equipmentNicknameTextView = view.findViewById(R.id.equipmentNickName)
        modelTextView = view.findViewById(R.id.equipmentModel)
        serialNumberTextView = view.findViewById(R.id.equipmentSerialNumber)
        manualsButton = view.findViewById(R.id.manualItem)
        guidesButton = view.findViewById(R.id.guidesItem)
        editSerialNumber = view.findViewById(R.id.editEquipmentIcon)
        engineHoursTextView = view.findViewById(R.id.engineHours)
        engineHoursButton = view.findViewById(R.id.engineHoursItem)
        faultCodeItem = view.findViewById(R.id.faultCodeItem)
        locationLabel = view.findViewById(R.id.locationLabel)
        locationTextView = view.findViewById(R.id.locationTextView)
        statusLabel = view.findViewById(R.id.statusLabel)
        statusTextView = view.findViewById(R.id.statusTextView)
        locationStatusBarrier = view.findViewById(R.id.locationStatusBarrier)
        batteryLabel = view.findViewById(R.id.batteryLabel)
        batteryGaugeView = view.findViewById(R.id.batteryGauge)
        fuelLabel = view.findViewById(R.id.fuelLabel)
        fuelGaugeView = view.findViewById(R.id.fuelGauge)
        defLabel = view.findViewById(R.id.defLabel)
        defGaugeView = view.findViewById(R.id.defGauge)
        gaugesBarrier = view.findViewById(R.id.gaugeBarrier)
        telematicsButton = view.findViewById(R.id.telematicsItem)
        telematicsDivider = view.findViewById(R.id.telematicsDivider)
        telematicsViewsGroup = view.findViewById(R.id.group)

        engineHoursButton.setOnClickListener {
            this.equipmentUnitId?.let {
                flowActivity?.addFragmentToBackStack(
                    EngineHoursFragment.createInstance(it)
                )
            }
        }
        hoursToNextServiceButton = view.findViewById(R.id.maintenanceItem)
        hoursToNextServiceButton.setOnClickListener {
            this.equipmentUnitId?.let {
                flowActivity?.addFragmentToBackStack(
                    HoursToServiceFragment.createInstance(it)
                )
            }
        }

        faultCodeItem.setOnClickListener {
            this.equipmentUnitId?.let {
                flowActivity?.addFragmentToBackStack(
                    FaultCodeInquiryFragment.createInstance(it)
                )
            }
        }
    }

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let { this.onBindData(it) }
        })

        this.viewModel.guideUrl.observe(viewLifecycleOwner, Observer { guideUrl ->
            val visible = guideUrl != null && this.viewModel.equipmentUnit.value != null
            this.guidesButton.visibility = if (visible) View.VISIBLE else View.GONE
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        activity?.title = display.nickname
        equipmentNicknameTextView.text = display.nickname
        modelImageView.setImageResource(display.imageResId)
        modelTextView.text = display.modelName
        serialNumberTextView.text = display.serialNumber
        engineHoursTextView.text = display.engineHours

        manualsButton.visibility = if (unit.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(
                ModelManualFragment.createInstance(unit.model)
            )
        }

        guidesButton.visibility = View.GONE
        AppProxy.proxy.serviceManager.equipmentService.getModel(model = unit.model)
                .done {
                    guidesButton.visibility = it?.guideUrl?.let {View.VISIBLE } ?: View.GONE
                }

        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(
                    GuidesListFragment.createInstance(unit.model)
                )
            } else {
                val fragment =
                    DisclaimerFragment.createInstance(
                        DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED
                    )
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        parentFragmentManager.popBackStack()
                        flowActivity?.addFragmentToBackStack(
                            GuidesListFragment.createInstance(unit.model)
                        )
                    }

                    override fun onDisclaimerDeclined() {
                        parentFragmentManager.popBackStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        editSerialNumber.setOnClickListener {
            val dialogFragment =
                EditSerialNumberDialogFragment.createDialogFragmentInstance(
                    unit.serial
                )
            dialogFragment.setTargetFragment(this,
                SERIAL_NUMBER_EDIT_REQUEST_CODE
            )
            dialogFragment.show(parentFragmentManager,
                EditSerialNumberDialogFragment.TAG
            )
        }

        if (unit.hasTelematics) {
            locationTextView.text = getString(R.string.not_available)

            unit.batteryVoltage?.let {batteryVolt ->
                batteryGaugeView.setPercent(batteryVolt)
            }

            unit.fuelLevelPercent?.let {fuelLevel ->
                fuelGaugeView.setPercent(fuelLevel.toDouble())
            }

            unit.defLevelPercent?.let {defLevel ->
                defGaugeView.setPercent(defLevel.toDouble())
            }

            telematicsViewsGroup.visibility = View.VISIBLE
            showTelematicsViews()
        }
        else {
            hideTelematicsViews()
        }
    }

    private fun showTelematicsViews() {
        telematicsViewsGroup.visibility = View.VISIBLE
        telematicsButton.visibility = View.VISIBLE
        telematicsDivider.visibility = View.VISIBLE
    }

    private fun hideTelematicsViews() {
        telematicsViewsGroup.visibility = View.GONE
        telematicsButton.visibility = View.GONE
        telematicsDivider.visibility = View.GONE
    }
}

private const val SERIAL_NUMBER_KEY = "serialNumber"

class EditSerialNumberDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EditSerialNumberDialogFragment"

        fun createDialogFragmentInstance(serialNumber: String?): EditSerialNumberDialogFragment {
            val args = Bundle(1)
            args.putString(SERIAL_NUMBER_KEY, serialNumber)
            val fragment =
                EditSerialNumberDialogFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var serialNumberTextView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val bodyView = inflater.inflate(R.layout.dialog_edit_serial_number, null)
        serialNumberTextView = bodyView.findViewById(R.id.serialNumber)
        val serialNumber = arguments?.getString(SERIAL_NUMBER_KEY)
        serialNumber?.let {
            serialNumberTextView.text = it
        }

        return AlertDialog.Builder(requireContext())
            .setView(bodyView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val data = Intent()
                val newSerialNumber =
                    if (serialNumberTextView.text.isEmpty()) null else serialNumberTextView.text.toString()
                data.putExtra(SERIAL_NUMBER_KEY, newSerialNumber)
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
                dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
