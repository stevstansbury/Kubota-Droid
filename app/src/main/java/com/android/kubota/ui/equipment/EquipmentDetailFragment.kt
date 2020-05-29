package com.android.kubota.ui.equipment

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Observer
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.displayInfo
import com.android.kubota.extensions.hasManual
import com.android.kubota.ui.*
import com.android.kubota.utility.AccountPrefs
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.EquipmentUnit
import java.util.*


class EquipmentDetailFragment : BaseEquipmentUnitFragment() {

    override val layoutResId: Int = R.layout.fragment_equipment_detail

    private lateinit var machineCard: MachineCardView
    private lateinit var manualsButton: View
    private lateinit var manualsDivider: View
    private lateinit var guidesButton: View
    private lateinit var guidesDivider: View
    private lateinit var faultCodeButton: View
    private lateinit var faultCodeChevron: ImageView
    private lateinit var telematicsButton: View
    private lateinit var telematicsDivider: View
    private lateinit var telematicsChevron: ImageView
    private lateinit var maintenanceScheduleButton: View

    companion object {

        fun createInstance(equipmentId: UUID): EquipmentDetailFragment {
            return EquipmentDetailFragment().apply {
                arguments = getBundle(equipmentId)
            }
        }
    }

    override fun initUi(view: View) {
        machineCard = view.findViewById(R.id.machineCardView)
        manualsButton = view.findViewById(R.id.manualsButton)
        manualsDivider = view.findViewById(R.id.manualDivider)
        guidesButton = view.findViewById(R.id.guidesButton)
        guidesDivider = view.findViewById(R.id.guidesDivider)
        telematicsButton = view.findViewById(R.id.telematicsButton)
        telematicsDivider = view.findViewById(R.id.telematicsDivider)
        telematicsChevron = view.findViewById(R.id.telematicsChevron)
        faultCodeButton = view.findViewById(R.id.faultCodeButton)
        faultCodeChevron = view.findViewById(R.id.faultCodeChevron)
        faultCodeButton.setOnClickListener {
            this.equipmentUnitId?.let {
                flowActivity?.addFragmentToBackStack(
                    FaultCodeInquiryFragment.createInstance(it)
                )
            }
        }
        maintenanceScheduleButton = view.findViewById(R.id.maintenanceSchedulesButton)

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

        this.viewModel.guideUrl.observe(viewLifecycleOwner, Observer { guideUrl ->
            val visible = guideUrl != null && this.viewModel.equipmentUnit.value != null
            this.guidesButton.visibility = if (visible) View.VISIBLE else View.GONE
            guidesDivider.visibility = guidesButton.visibility
        })
    }

    private fun onBindData(unit: EquipmentUnit) {
        val display = unit.displayInfo(context = this)
        activity?.title = display.nickname

        machineCard.setOnEditViewClicked (object: MachineCardView.OnEditViewClicked {
            override fun onClick() {
                flowActivity?.addFragmentToBackStack(EditEquipmentFragment.createInstance(unit.id))
            }
        })

        telematicsChevron.setImageResource(
            if (unit.hasTelematics)
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        faultCodeChevron.setImageResource(
            if (unit.faultCodes.isNotEmpty())
                R.drawable.ic_chevron_right_red_dot
            else
                R.drawable.ic_chevron_right_24dp
        )
        manualsButton.visibility = if (unit.hasManual) View.VISIBLE else View.GONE
        manualsDivider.visibility = manualsButton.visibility
        manualsButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(ManualsListFragment.createInstance(modelName = unit.model))
        }

        guidesButton.visibility = View.GONE
        guidesDivider.visibility = guidesButton.visibility
        AppProxy.proxy.serviceManager.equipmentService.getModel(model = unit.model)
                .done {
                    guidesButton.visibility = it?.guideUrl?.let {View.VISIBLE } ?: View.GONE
                }
                .catch {
                    manualsButton.visibility = View.GONE
                }
                .finally {
                    guidesDivider.visibility = guidesButton.visibility
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

        maintenanceScheduleButton.setOnClickListener {
            flowActivity?.addFragmentToBackStack(MaintenanceIntervalFragment.createInstance(unit.model))
        }
    }


}
