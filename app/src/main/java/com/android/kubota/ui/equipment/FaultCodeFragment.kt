package com.android.kubota.ui.equipment

import android.os.Bundle
import android.view.View
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.BaseFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.cauterize
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit

class FaultCodeFragment : BaseFragment() {
    companion object {
        private const val EQUIPMENT_UNIT_KEY = "EQUIPMENT_UNIT_KEY"
        private const val EQUIPMENT_MODEL_KEY = "EQUIPMENT_MODEL_KEY"

        fun createInstance(equipmentUnit: EquipmentUnit): FaultCodeFragment {
            return FaultCodeFragment().apply {
                val data = Bundle(1)
                data.put(EQUIPMENT_UNIT_KEY, equipmentUnit)
                arguments = data
            }
        }

        fun createInstance(equipmentModel: EquipmentModel): FaultCodeFragment {
            return FaultCodeFragment().apply {
                val data = Bundle(1)
                data.put(EQUIPMENT_MODEL_KEY, equipmentModel)
                arguments = data
            }
        }
    }

    override val layoutResId: Int = R.layout.fragment_fault_code

    private var equipmentUnit: EquipmentUnit? = null
    private var equipmentModel: EquipmentModel? = null

    private val modelName: String
        get() = this.equipmentUnit?.model ?: this.equipmentModel?.model
        ?: throw IllegalStateException()

    private var onLookupScreen: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onLookupScreen = savedInstanceState?.getBoolean("onLookupScreen")
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.fault_code_screen_title)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("onLookupScreen", childFragmentManager.fragments.firstOrNull() is FaultCodeLookupFragment)
        super.onSaveInstanceState(outState)
    }

    override fun hasRequiredArgumentData(): Boolean {
        this.equipmentUnit = arguments?.getT(EQUIPMENT_UNIT_KEY)
        this.equipmentModel = arguments?.getT(EQUIPMENT_MODEL_KEY)

        return this.equipmentUnit != null || this.equipmentModel != null
    }

    override fun initUi(view: View) {
        when {
            equipmentUnit?.telematics == null -> {
                view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup).visibility = View.GONE
                view.findViewById<View>(R.id.conditionalPadding).visibility = View.VISIBLE
                showFaultLookup()
            }
            onLookupScreen == true -> showFaultLookup()
            else -> showActiveFaults()
        }

        view.findViewById<MaterialButton>(R.id.activeCodesButton).setOnClickListener {
            showActiveFaults()
        }

        view.findViewById<MaterialButton>(R.id.codeLookupButton).setOnClickListener {
            showFaultLookup()
        }
    }

    private fun showActiveFaults() {
        check(equipmentModel == null)
        val existingFragment = childFragmentManager.fragments.firstOrNull()

        if (existingFragment is FaultCodeListFragment) {
            return
        }

        showProgressBar()
        AppProxy.proxy.serviceManager.faultService.getRecentCodes(equipmentUnit!!.id)
            .done {
                val fragment = FaultCodeListFragment.createInstance(it)
                childFragmentManager
                    .beginTransaction()
                    .replace(R.id.faultChildFragmentContainer, fragment)
                    .commit()
            }
            .ensure { hideProgressBar() }
            .cauterize()
    }

    private fun showFaultLookup() {
        val existingFragment = childFragmentManager.fragments.firstOrNull()

        if (existingFragment is FaultCodeLookupFragment) {
            return
        }

        val fragment = FaultCodeLookupFragment.createInstance(
            modelName = modelName,
            activeFaults = equipmentUnit?.telematics?.faultCodes ?: emptyList()
        )
        childFragmentManager
            .beginTransaction()
            .replace(R.id.faultChildFragmentContainer, fragment)
            .commit()
    }

    override fun loadData() {}
}