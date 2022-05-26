package com.android.kubota.ui.equipment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.android.kubota.R
import com.android.kubota.extensions.*
import com.android.kubota.ui.*
import com.android.kubota.ui.equipment.filter.EquipmentTreeFilterFragment
import com.android.kubota.ui.geofence.GeofenceFragment
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.showMessage
import com.android.kubota.viewmodel.equipment.EquipmentUnitNotifyUpdateViewModel
import com.inmotionsoftware.promisekt.map
import com.kubota.service.domain.*

class EquipmentDetailFragment : BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var machineCard: MachineCardView
    private lateinit var manualsButton: TextView
    private lateinit var guidesButton: TextView
    private lateinit var faultCodeButton: TextView
    private lateinit var inhibitRestartButton: TextView
    private lateinit var telematicsButton: TextView
    private lateinit var geofenceButton: TextView
    private lateinit var maintenanceScheduleButton: TextView
    private lateinit var warrantyInfoButton: TextView
    private lateinit var instructionalVideoButton: TextView
    private lateinit var compatibleWithButton: TextView
    private lateinit var kubotaNowHeader: TextView
    private lateinit var attachmentSliderView: AttachmentsSliderView

    private val notifyUpdateViewModel: EquipmentUnitNotifyUpdateViewModel by lazy {
        EquipmentUnitNotifyUpdateViewModel.instance(owner = this.requireActivity())
    }

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
        geofenceButton = view.findViewById(R.id.geofenceButton)
        faultCodeButton = view.findViewById(R.id.faultCodeButton)
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
        compatibleWithButton = view.findViewById(R.id.compatibleWithButton)
        kubotaNowHeader = view.findViewById(R.id.kubota_new_header)

        attachmentSliderView = view.findViewById(R.id.attachment_slider)
        attachmentSliderView.setOnAttachmentClickedListener(object :
            AttachmentsSliderView.OnAttachmentClicked {
            override fun onItemClicked(attachmentItem: AttachmentsSliderView.AttachmentCategoryItemState) {
                viewModel.equipmentUnit.value?.let {
                    flowActivity?.addFragmentToBackStack(
                        EquipmentTreeFilterFragment.instance(
                            compatibleWithModel = it.model,
                            selectedCategories = listOf(attachmentItem.categoryName)
                        )
                    )
                }
            }

            override fun onSeeAllItemClicked(attachmentItem: AttachmentsSliderView.AttachmentCategoryItemState) {
                viewModel.equipmentUnit.value?.let {
                    flowActivity?.addFragmentToBackStack(
                        EquipmentTreeFilterFragment.instance(it.model, emptyList())
                    )
                }
            }
        })

        machineCard.enterDetailMode()
    }

    @SuppressLint("MissingSuperCall")
    override fun loadData() {
        this.hideProgressBar()

        this.viewModel.isLoading.observe(this) { loading ->
            when (loading) {
                true -> this.showBlockingActivityIndicator()
                else -> this.hideBlockingActivityIndicator()
            }
        }

        this.viewModel.error.observe(this) { error ->
            error?.let { this.showError(it) }
        }

        this.viewModel.equipmentUnit.observe(this) { unit ->
            unit?.let {
                onBindData(it)
                machineCard.setModel(it)
            }
        }

        when (this.equipmentUnit?.type) {
            EquipmentModel.Type.Machine -> {
                compatibleWithButton.text =
                    getString(R.string.compatible_attachments)

                viewModel.loadCompatibleAttachments()
                this.viewModel.compatibleAttachments.observe(viewLifecycleOwner) { list ->
                    if (list.isNotEmpty()) {
                        attachmentSliderView.displayCompatibleAttachments(list)
                        attachmentSliderView.isVisible = true
                    }

                    compatibleWithButton.isVisible = list.isNotEmpty()
                }
            }

            EquipmentModel.Type.Attachment -> {
                compatibleWithButton.text =
                    getString(R.string.compatible_machines)

                viewModel.loadCompatibleMachines()
                viewModel.compatibleMachines.observe(viewLifecycleOwner) { list ->
                    compatibleWithButton.isVisible = list.isNotEmpty()
                }
            }
        }

        this.notifyUpdateViewModel.unitUpdated.observe(viewLifecycleOwner) { didUpdate ->
            if (didUpdate) {
                viewModel.reload(authDelegate)
            }
        }
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        activity?.title = display.nickname

        machineCard.setOnLocationViewClicked(object : MachineCardView.OnLocationViewClicked {
            override fun onClick() {
                geofenceButton.callOnClick()
            }
        })

        val geofenceDrawable = if (unit.telematics?.outsideGeofence == true) {
            requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
        } else {
            requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        }

        geofenceButton.setCompoundDrawablesWithIntrinsicBounds(null, null, geofenceDrawable, null)
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

        var telematicsStatus = requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        unit.telematics?.let {
            val status = TelematicStatus.Critical
            if (it.voltageStatus == status || it.defRemainingStatus == status || it.fuelRemainingStatus == status || it.hydraulicTempStatus == status || it.coolantTempStatus == status) {
                telematicsStatus = requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
            }
        }

        telematicsButton.setCompoundDrawablesWithIntrinsicBounds(null, null, telematicsStatus, null)

        kubotaNowHeader.isVisible = unit.hasTelematics || unit.canModifyRestart()

        faultCodeButton.visibility = if (unit.hasFaultCodes) View.VISIBLE else View.GONE

        val faultCodeDrawable = if (unit.telematics?.faultCodes?.isNotEmpty() == true) {
            requireContext().getDrawable(R.drawable.ic_chevron_right_red_dot)
        } else {
            requireContext().getDrawable(R.drawable.ic_chevron_right_24dp)
        }

        faultCodeButton.setCompoundDrawablesWithIntrinsicBounds(null, null, faultCodeDrawable, null)

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

        warrantyInfoButton.visibility =
            if (unit.warrantyUrl != null && unit.type == EquipmentModel.Type.Machine) View.VISIBLE else View.GONE
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

        compatibleWithButton.setOnClickListener {
            viewModel.equipmentUnit.value?.let {
                flowActivity?.addFragmentToBackStack(
                    EquipmentTreeFilterFragment.instance(it.model, emptyList())
                )
            }
        }
    }
}

private fun EquipmentUnit.canModifyRestart() = telematics?.restartInhibitStatus?.canModify == true
