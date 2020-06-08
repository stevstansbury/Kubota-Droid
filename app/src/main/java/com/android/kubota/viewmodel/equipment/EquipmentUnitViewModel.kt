package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.SignInHandler
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.FaultCode
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*

class EquipmentUnitViewModelFactory(
    private val equipmentUnitId: UUID,
    private val faultCodes: List<Int>?,
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnitId, faultCodes, signInHandler ) as T
    }

}

class EquipmentUnitViewModel(
    private val equipmentUnitId: UUID,
    private val faultCodes: List<Int>?,
    private val signInHandler: WeakReference<SignInHandler>?
) : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner,
                     equipmentUnitId: UUID,
                     faultCodes: List<Int>?,
                     signInHandler: WeakReference<SignInHandler>?
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnitId, faultCodes, signInHandler))
                        .get(EquipmentUnitViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentUnit = MutableLiveData<EquipmentUnit?>(null)
    private val mGuideUrl = MutableLiveData<URL?>(null)
    private val mEngineHoursSaved = MutableLiveData(false)
    private val mFaultCodes = MutableLiveData<List<FaultCode>?>(null)

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
                    .onSignIn { signIn() }
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = this.equipmentUnitId) }
                    .done { unit ->
                        mEquipmentUnit.value = unit
                        unit?.let {
                            mGuideUrl.postValue(it.guideUrl)
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
            .onSignIn { signIn() }
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(type = EquipmentUnitUpdateType.UnverifiedEngineHours(this.equipmentUnitId, hours))
            }
            .done { mEngineHoursSaved.value = true }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }

    private fun getFaultCodes(unit: EquipmentUnit) {
        if (unit.faultCodes.isEmpty()) return
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.equipmentService.getFaultCodes(model = unit.model, codes = unit.faultCodes.map { "$it" })
            .done { mFaultCodes.value = it }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

    private fun signIn(): Promise<Unit> {
        return signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }

}