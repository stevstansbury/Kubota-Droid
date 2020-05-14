package com.android.kubota.ui.equipment

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.android.kubota.R
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.imageResId
import com.android.kubota.ui.AccountController
import com.android.kubota.ui.BaseFragment
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentUnit
import java.util.*

abstract class BaseEquipmentUnitFragment : BaseFragment() {
    companion object {
        private const val EQUIPMENT_KEY = "EQUIPMENT_KEY"

        fun getBundle(equipmentId: UUID): Bundle {
            val data = Bundle()
            data.putString(EQUIPMENT_KEY, equipmentId.toString())
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
    protected var equipmentUnit: EquipmentUnit? = null

    @CallSuper
    override fun hasRequiredArgumentData(): Boolean {
        return arguments?.getString(EQUIPMENT_KEY)?.let {
            equipmentUnitId = UUID.fromString(it)
            true
        } ?: false
    }

    @CallSuper
    override fun loadData() {
        val equipmentId = this.equipmentUnitId ?: return
        this.flowActivity?.showProgressBar()
        AuthPromise()
            .onSignIn { this.signIn() }
            .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = equipmentId) }
            .done { unit ->
                this.equipmentUnit = unit
                unit?.let { this.onDataLoaded(it) }
            }
            .ensure { this.flowActivity?.hideProgressBar() }
            .catch {
                this.flowActivity?.makeSnackbar()?.setText(R.string.server_error_message)?.show()
            }
    }

    protected fun signIn() {
        (activity as? AccountController)?.signIn()
    }

    protected abstract fun onDataLoaded(unit: EquipmentUnit)

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
