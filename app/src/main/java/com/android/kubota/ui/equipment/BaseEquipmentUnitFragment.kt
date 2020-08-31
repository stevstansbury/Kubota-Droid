package com.android.kubota.ui.equipment

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.Observer
import com.android.kubota.ui.AuthBaseFragment
import com.android.kubota.viewmodel.equipment.EquipmentUnitViewModel
import com.inmotionsoftware.flowkit.android.getT
import com.inmotionsoftware.flowkit.android.put
import com.kubota.service.domain.EquipmentUnit

abstract class BaseEquipmentUnitFragment : AuthBaseFragment() {

    companion object {
        const val EQUIPMENT_KEY = "EQUIPMENT_UNIT_KEY"

        fun getBundle(equipmentUnit: EquipmentUnit): Bundle {
            val data = Bundle(1)
            data.put(EQUIPMENT_KEY, equipmentUnit)
            return data
        }
    }
    
    data class EquipmentUnitDisplayInfo(
        val imageResId: Int,
        val modelName: String,
        val serialNumber: String,
        val nickname: String,
        val engineHours: String
    )

    protected var equipmentUnit: EquipmentUnit? = null

    protected val viewModel: EquipmentUnitViewModel by lazy {
        EquipmentUnitViewModel.instance(
            owner = this,
            equipmentUnit = this.equipmentUnit!!
        )
    }

    @CallSuper
    override fun hasRequiredArgumentData(): Boolean {
        this.equipmentUnit = arguments?.getT(EQUIPMENT_KEY)
        return this.equipmentUnit != null
    }

    @CallSuper
    override fun loadData() {
        this.viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            when (loading) {
                true -> this.showProgressBar()
                else -> this.hideProgressBar()
            }
        })

        this.viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let { this.showError(it) }
        })
    }
}
