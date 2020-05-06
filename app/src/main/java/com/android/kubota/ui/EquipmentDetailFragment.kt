package com.android.kubota.ui

import android.app.Activity
import android.app.Dialog
import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.R
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.InjectorUtils
import com.android.kubota.viewmodel.EquipmentDetailViewModel
import com.kubota.repository.uimodel.Equipment

private const val EQUIPMENT_KEY = "equipment"

class EquipmentDetailFragment: BaseFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var viewModel: EquipmentDetailViewModel
    private lateinit var equipment: Equipment

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

        fun createInstance(equipmentId: Int): EquipmentDetailFragment {
            val data = Bundle(1)
            data.putInt(EQUIPMENT_KEY, equipmentId)

            val fragment = EquipmentDetailFragment()
            fragment.arguments = data

            return fragment
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SERIAL_NUMBER_EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val newSerialNumber = data?.getStringExtra(SERIAL_NUMBER_KEY)
            viewModel.updateSerialNumber(equipment.id, newSerialNumber)

            return
        }

        return super.onActivityResult(requestCode, resultCode, data)
    }

    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getInt(EQUIPMENT_KEY)?.let {equipmentId ->
            InjectorUtils.provideEquipmentDetailViewModel(requireContext(), equipmentId).apply {
                viewModel = ViewModelProvider(this@EquipmentDetailFragment, this)
                    .get(EquipmentDetailViewModel::class.java)
            }

            equipmentId > 0
        }
            ?: false
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
            flowActivity?.addFragmentToBackStack(EngineHoursFragment.createInstance(this.equipment.id))
        }
        hoursToNextServiceButton = view.findViewById(R.id.maintenanceItem)
        hoursToNextServiceButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(HoursToServiceFragment.createInstance(this.equipment.id))
        }

        faultCodeItem.setOnClickListener {
            flowActivity?.addFragmentToBackStack(FaultCodeInquiryFragment.createInstance(this.equipment.id))
        }
    }

    override fun loadData() {
        viewModel.liveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                updateUI(it)
            } else {
                requireActivity().onBackPressed()
            }
        })
    }

    private fun updateUI(equipment: Equipment) {
        this.equipment = equipment

        val equipmentNickname =
            if (equipment.nickname.isNullOrBlank())
                getString(R.string.no_equipment_name_fmt, equipment.model)
            else
                equipment.nickname

        equipmentNicknameTextView.text = equipmentNickname
        activity?.title = equipmentNickname

        val imageResId = CategoryUtils.getEquipmentImage(equipment.category, equipment.model)
        if (imageResId != 0) {
            modelImageView.setImageResource(imageResId)
        }

        modelTextView.text = equipment.model
        serialNumberTextView.text = if (equipment.serialNumber != null) {
            getString(R.string.equipment_serial_number_fmt, equipment.serialNumber)
        } else {
            getString(R.string.equipment_serial_number)
        }

        engineHoursTextView.text = equipment.engineHours.toString()

        manualsButton.visibility = if (equipment.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ModelManualFragment.createInstance(equipment.id, equipment.model))
        }

        guidesButton.visibility = if (equipment.hasMaintenanceGuides) View.VISIBLE else View.GONE
        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(equipment.model))
            } else {
                val fragment = DisclaimerFragment.createInstance(DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED)
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        parentFragmentManager.popBackStack()
                        flowActivity?.addFragmentToBackStack(GuidesListFragment.createInstance(equipment.model))
                    }

                    override fun onDisclaimerDeclined() {
                        parentFragmentManager.popBackStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        editSerialNumber.setOnClickListener {
            val dialogFragment = EditSerialNumberDialogFragment.createDialogFragmentInstance(equipment.serialNumber)
            dialogFragment.setTargetFragment(this, SERIAL_NUMBER_EDIT_REQUEST_CODE)
            dialogFragment.show(parentFragmentManager, EditSerialNumberDialogFragment.TAG)
        }

        equipment.telematics
            ?.let {
                locationTextView.text = getString(R.string.not_available)

                it.batteryVoltage?.let {batteryVolt ->
                    batteryGaugeView.setPercent(batteryVolt)
                }

                it.fuelLevel?.let {fuelLevel ->
                    fuelGaugeView.setPercent(fuelLevel)
                }

                it.defLevel?.let {defLevel ->
                    defGaugeView.setPercent(defLevel)
                }

                telematicsViewsGroup.visibility = View.VISIBLE
                showTelematicsViews()
            }
            ?: hideTelematicsViews()
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
            val fragment = EditSerialNumberDialogFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var serialNumberTextView: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val bodyView = inflater.inflate(R.layout.dialog_edit_serial_number, null)
        serialNumberTextView =  bodyView.findViewById(R.id.serialNumber)
        val serialNumber = arguments?.getString(SERIAL_NUMBER_KEY)
        serialNumber?.let {
            serialNumberTextView.text = it
        }

        return AlertDialog.Builder(requireContext())
            .setView(bodyView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val data = Intent()
                val newSerialNumber = if (serialNumberTextView.text.isEmpty()) null else serialNumberTextView.text.toString()
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