package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.extensions.displayInfo
import com.android.kubota.extensions.hasInstrucationalVideo
import com.android.kubota.extensions.hasManual
import com.android.kubota.extensions.hasTelematics
import com.android.kubota.ui.*
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.showMessage
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.*


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
    private lateinit var instructionalVideoButton: View

    private var shouldReload = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (shouldReload) {
            viewModel.reload(authDelegate)
        } else {
            shouldReload = true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            viewModel.equipmentUnit.value?.displayName?.let { activity?.title = it }
        }
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
                    FaultCodeFragment.createInstance(it)
                )
            }
        }
        maintenanceScheduleButton = view.findViewById(R.id.maintenanceSchedulesButton)
        inhibitRestartButton = view.findViewById(R.id.inhibitRestartButton)
        warrantyInfoButton = view.findViewById(R.id.warrantyInfoButton)
        instructionalVideoButton = view.findViewById(R.id.instructionalVideoButton)

        machineCard.enterDetailMode()
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        this.hideProgressBar()

        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            when (loading) {
                true -> this.showBlockingActivityIndicator()
                else -> this.hideBlockingActivityIndicator()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })

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

        machineCard.setOnLocationViewClicked(object : MachineCardView.OnLocationViewClicked {
            override fun onClick() {
                geofenceButton.callOnClick()
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

        machineCard.setOnEditViewClicked(object : MachineCardView.OnEditViewClicked {
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

        var telematicsStatus = R.drawable.ic_chevron_right_24dp
        unit.telematics?.let {
            val status = TelematicStatus.Critical
            if (it.voltageStatus == status || it.defRemainingStatus == status || it.fuelRemainingStatus == status || it.hydraulicTempStatus == status || it.coolantTempStatus == status) {
                telematicsStatus = R.drawable.ic_chevron_right_red_dot
            }
        }
        telematicsChevron.setImageResource(telematicsStatus)

        faultCodeButton.visibility = if (unit.hasFaultCodes) View.VISIBLE else View.GONE
        faultCodeChevron.setImageResource(
            if (unit.telematics?.faultCodes?.isNotEmpty() == true)
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        manualsButton.visibility = if (unit.hasManual) View.VISIBLE else View.GONE
        manualsButton.setOnClickListener {
            when (unit.manualInfo.count() == 1) {
                true -> this.flowActivity?.let {
                    ManualsListFragment.pushManualToStack(it, unit.manualInfo.first())
                }
                false -> this.flowActivity?.addFragmentToBackStack(
                    ManualsListFragment.createInstance(
                        modelName = unit.model,
                        manualInfo = unit.manualInfo
                    )
                )
            }
        }

        guidesButton.visibility = if (unit.guideUrl != null) View.VISIBLE else View.GONE
        guidesButton.setOnClickListener {
            if (AccountPrefs.getIsDisclaimerAccepted(requireContext())) {
                flowActivity?.addFragmentToBackStack(
                    GuidesListFragment.createInstance(unit.model)
                )
            } else {
                val fragment = DisclaimerFragment.createInstance(
                    DisclaimerFragment.VIEW_MODE_RESPONSE_REQUIRED
                )
                fragment.setDisclaimerInterface(object : DisclaimerInterface {
                    override fun onDisclaimerAccepted() {
                        activity?.popCurrentTabStack()
                        flowActivity?.addFragmentToBackStack(
                            GuidesListFragment.createInstance(unit.model)
                        )
                    }

                    override fun onDisclaimerDeclined() {
                        activity?.popCurrentTabStack()
                    }
                })
                flowActivity?.addFragmentToBackStack(fragment)
            }
        }

        maintenanceScheduleButton.visibility =
            if (unit.hasMaintenanceSchedules) View.VISIBLE else View.GONE
        maintenanceScheduleButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(MaintenanceIntervalFragment.createInstance(unit.model))
        }

        warrantyInfoButton.visibility = if (unit.warrantyUrl != null) View.VISIBLE else View.GONE
        unit.warrantyUrl?.let { warrantyUrl ->
            warrantyInfoButton.setOnClickListener {
                showMessage(
                    titleId = R.string.leave_app_dialog_title,
                    messageId = R.string.leave_app_kubota_usa_website_msg
                )
                    .map { idx ->
                        if (idx != AlertDialog.BUTTON_POSITIVE) return@map
                        val url = warrantyUrl.toString()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
            }
        }

        instructionalVideoButton.visibility =
            if (unit.hasInstrucationalVideo) View.VISIBLE else View.GONE
        instructionalVideoButton.setOnClickListener {
            this.flowActivity?.addFragmentToBackStack(
                VideoListFragment.createInstance(
                    modelName = unit.model,
                    videoInfo = unit.instructionalVideos
                )
            )
        }
    }
}

private fun EquipmentUnit.canModifyRestart() = telematics?.restartInhibitStatus?.canModify == true
