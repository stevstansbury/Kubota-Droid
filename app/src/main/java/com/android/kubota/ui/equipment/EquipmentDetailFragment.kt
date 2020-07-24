package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.displayInfo
import com.android.kubota.extensions.hasManual
import com.android.kubota.extensions.hasTelematics
import com.android.kubota.ui.*
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.utility.AccountPrefs
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.manualInfo
import com.kubota.service.domain.outsideGeofence


class EquipmentDetailFragment : BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var machineCard: MachineCardView
    private lateinit var manualsButton: View
    private lateinit var guidesButton: View
    private lateinit var faultCodeButton: View
    private lateinit var faultCodeChevron: ImageView
    private lateinit var inhibitRestartButton: View
    private lateinit var telematicsButton: View
    private lateinit var telematicsChevron: ImageView
    private lateinit var geofenceButton: View
    private lateinit var geofenceChevron: ImageView
    private lateinit var maintenanceScheduleButton: View
    private lateinit var warrantyInfoButton: View

    companion object {
        fun createInstance(equipmentUnit: EquipmentUnit): EquipmentDetailFragment {
            return EquipmentDetailFragment().apply {
                arguments = getBundle(equipmentUnit)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun initUi(view: View) {
        machineCard = view.findViewById(R.id.machineCardView)
        manualsButton = view.findViewById(R.id.manualsButton)
        guidesButton = view.findViewById(R.id.guidesButton)
        telematicsButton = view.findViewById(R.id.telematicsButton)
        telematicsChevron = view.findViewById(R.id.telematicsChevron)
        geofenceButton = view.findViewById(R.id.geofenceButton)
        geofenceChevron = view.findViewById(R.id.geofenceChevron)
        faultCodeButton = view.findViewById(R.id.faultCodeButton)
        faultCodeChevron = view.findViewById(R.id.faultCodeChevron)
        faultCodeButton.setOnClickListener {
            this.equipmentUnit?.let {
                flowActivity?.addFragmentToBackStack(
                    FaultCodeInquiryFragment.createInstance(it)
                )
            }
        }
        maintenanceScheduleButton = view.findViewById(R.id.maintenanceSchedulesButton)
        inhibitRestartButton = view.findViewById(R.id.inhibitRestartButton)
        warrantyInfoButton = view.findViewById(R.id.warrantyInfoButton)

        machineCard.enterDetailMode()
    }

    override fun loadData() {
        super.loadData()

        this.viewModel.equipmentUnit.observe(viewLifecycleOwner, Observer { unit ->
            unit?.let {
                onBindData(it)
                machineCard.setModel(it)
            }
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        activity?.title = display.nickname

        machineCard.setOnLocationViewClicked (object: MachineCardView.OnLocationViewClicked {
            override fun onClick() {
//                geofenceButton.callOnClick()
            }
        })

        geofenceChevron.setImageResource(
            if (unit.telematics?.outsideGeofence == true)
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        geofenceButton.visibility = if (unit.hasTelematics) View.VISIBLE else View.GONE
        geofenceButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(GeofenceFragment.createInstance(unit.telematics?.location))
        }

        machineCard.setOnEditViewClicked (object: MachineCardView.OnEditViewClicked {
            override fun onClick() {
                flowActivity?.addFragmentToBackStack(EditEquipmentFragment.createInstance(unit))
            }
        })

        telematicsButton.visibility = if (unit.hasTelematics) View.VISIBLE else View.GONE
        telematicsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(TelematicsFragment.createInstance(unit.id))
        }

        inhibitRestartButton.visibility = if (unit.canModifyRestart()) View.VISIBLE else View.GONE
        inhibitRestartButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(InhibitStarterFragment.createInstance(unit))
        }

        telematicsChevron.setImageResource(
            if (unit.hasTelematics)
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        faultCodeButton.visibility = if (unit.hasFaultCodes) View.VISIBLE else View.GONE
        faultCodeChevron.setImageResource(
            if (unit.telematics?.faultCodes?.isNotEmpty() == true)
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        manualsButton.visibility = if (unit.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ManualsListFragment.createInstance(modelName = unit.model, manualInfo = unit.manualInfo))
        }

        guidesButton.visibility = if (unit.guideUrl != null) View.VISIBLE else View.GONE
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

        maintenanceScheduleButton.visibility = if (unit.hasMaintenanceSchedules) View.VISIBLE else View.GONE
        maintenanceScheduleButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(MaintenanceIntervalFragment.createInstance(unit.model))
        }

        warrantyInfoButton.visibility = if (unit.warrantyUrl != null) View.VISIBLE else View.GONE
    }
}

private fun EquipmentUnit.canModifyRestart() = telematics?.restartInhibitStatus?.canModify == true