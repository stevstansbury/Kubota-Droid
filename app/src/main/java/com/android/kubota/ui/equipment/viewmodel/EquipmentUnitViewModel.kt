package com.android.kubota.ui.equipment.viewmodel

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.FaultCode
import java.net.URL
import java.util.*

class EquipmentUnitViewModelFactory(
    private val equipmentUnitId: UUID,
    private val faultCodes: List<Int>?,
    private val signInHandler: (() -> Promise<Unit>)?
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnitId, faultCodes, signInHandler ) as T
    }

}

class EquipmentUnitViewModel(
    private val equipmentUnitId: UUID,
    private val faultCodes: List<Int>?,
    private val signInHandler: (() -> Promise<Unit>)?
) : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner,
                     equipmentUnitId: UUID,
                     faultCodes: List<Int>?,
                     signInHandler: (() -> Promise<Unit>)?
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnitId, faultCodes, signInHandler))
                        .get(EquipmentUnitViewModel::class.java)
        }
    }

    private var mIsLoading = MutableLiveData(false)
    private var mError = MutableLiveData<Throwable?>(null)
    private var mEquipmentUnit = MutableLiveData<EquipmentUnit?>(null)
    private var mGuideUrl = MutableLiveData<URL?>(null)
    private var mEngineHoursSaved = MutableLiveData(false)
    private var mFaultCodes = MutableLiveData<List<FaultCode>?>(null)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentUnit: LiveData<EquipmentUnit?> = mEquipmentUnit
    val guideUrl: LiveData<URL?> = mGuideUrl
    val engineHoursSaved: LiveData<Boolean> = mEngineHoursSaved
    val equipmentUnitFaultCodes: LiveData<List<FaultCode>?> = mFaultCodes

    init {
        this.updateEquipmentUnit()
    }

    fun updateEquipmentUnit() {
        when (AppProxy.proxy.accountManager.isAuthenticated.value ) {
            true -> {
                this.mIsLoading.value = true
                AuthPromise()
                    .onSignIn { onSignIn() }
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = this.equipmentUnitId) }
                    .done { unit ->
                        mEquipmentUnit.value = unit
                        unit?.let {
                            getGuideUrl(it)
                            getFaultCodes(it)
                        }
                    }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> {
                mEquipmentUnit.value = null
            }
        }
    }

    fun saveEngineHours(hours: Double) {
        this.mIsLoading.value = true
        this.mEngineHoursSaved.value = false

        AuthPromise()
            .onSignIn { onSignIn() }
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(type = EquipmentUnitUpdateType.UnverifiedEngineHours(this.equipmentUnitId, hours))
            }
            .done { mEngineHoursSaved.value = true }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }

    private fun getGuideUrl(unit: EquipmentUnit) {
        AppProxy.proxy.serviceManager.equipmentService.getModel(model = unit.model)
            .done { model ->
                model?.guideUrl?.let {
                    this.mGuideUrl.value = URL(it)
                }
            }
    }

    private fun getFaultCodes(unit: EquipmentUnit) {
        if (this.faultCodes == null) return
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.equipmentService.getFaultCodes(model = unit.model, codes = this.faultCodes.map { "$it" })
            .done { mFaultCodes.value = it }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

    private fun onSignIn(): Promise<Unit> {
        return signInHandler?.let { it() } ?: Promise.value(Unit)
    }

}