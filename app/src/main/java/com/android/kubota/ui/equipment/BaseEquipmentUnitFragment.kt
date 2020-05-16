package com.android.kubota.ui.equipment

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.android.kubota.R
import androidx.lifecycle.Observer
import com.android.kubota.extensions.imageResId
import com.android.kubota.ui.BaseFragment
import com.android.kubota.viewmodel.equipment.EquipmentUnitViewModel
import com.kubota.service.domain.EquipmentUnit
import java.lang.ref.WeakReference
import java.util.*

abstract class BaseEquipmentUnitFragment : BaseFragment() {

    companion object {
        private const val EQUIPMENT_KEY = "EQUIPMENT_KEY"
        private const val FAULT_CODES_KEY = "FAULT_CODES_KEY"

        fun getBundle(equipmentId: UUID, faultCodes: ArrayList<Int>? = null): Bundle {
            val data = Bundle()
            data.putString(EQUIPMENT_KEY, equipmentId.toString())

            faultCodes?.let {
                data.putIntegerArrayList(FAULT_CODES_KEY, it)
            }
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

    protected var equipmentUnitId: UUID? = null
    protected var faultCodes: List<Int>? = null

    protected val viewModel: EquipmentUnitViewModel by lazy {
        EquipmentUnitViewModel.instance(
            owner = this,
            equipmentUnitId = this.equipmentUnitId!!,
            faultCodes = this.faultCodes,
            signInHandler = WeakReference { this.signInAsync() }
        )
    }

    @CallSuper
    override fun hasRequiredArgumentData(): Boolean {
        faultCodes = arguments?.getIntegerArrayList(FAULT_CODES_KEY)

        return arguments?.getString(EQUIPMENT_KEY)?.let {
            equipmentUnitId = UUID.fromString(it)
            true
        } ?: false
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

    //
    // Extension
    //
    protected fun EquipmentUnit.displayInfo(context: Fragment): EquipmentUnitDisplayInfo {
        return EquipmentUnitDisplayInfo(
            imageResId = this.imageResId,
            modelName = this.model,
            serialNumber = if (this.serial.isNullOrBlank()) {
                                context.getString(R.string.equipment_serial_number)
                            } else {
                                context.getString(R.string.equipment_serial_number_fmt, this.serial)
                            },
            nickname = if (this.nickName.isNullOrBlank()) getString(R.string.no_equipment_name_fmt, this.model) else this.nickName!!,
            engineHours = "${this.engineHours ?: 0.0}"
        )
    }
}
