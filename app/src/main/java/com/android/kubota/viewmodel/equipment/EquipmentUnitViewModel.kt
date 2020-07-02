package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.domain.EquipmentUnit
import java.util.*

class EquipmentUnitViewModelFactory(
    private val equipmentUnitId: UUID
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnitId) as T
    }

}

class EquipmentUnitViewModel(
    private val equipmentUnitId: UUID
) : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner,
                     equipmentUnitId: UUID
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnitId))
                        .get(EquipmentUnitViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentUnit = MutableLiveData<EquipmentUnit?>(null)
    private val mEngineHoursSaved = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentUnit: LiveData<EquipmentUnit?> = mEquipmentUnit
    val engineHoursSaved: LiveData<Boolean> = mEngineHoursSaved

    init {
        this.updateEquipmentUnit(delegate = null)
    }

    fun updateEquipmentUnit(delegate: AuthDelegate?) {
        when (AppProxy.proxy.accountManager.isAuthenticated.value ) {
            true -> {
                this.mIsLoading.value = true
                AuthPromise(delegate)
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(id = this.equipmentUnitId) }
                    .done { mEquipmentUnit.value = it }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> {
                mEquipmentUnit.value = null
            }
        }
    }

    fun saveEngineHours(delegate: AuthDelegate?, hours: Double) {
        this.mIsLoading.value = true
        this.mEngineHoursSaved.value = false

        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(type = EquipmentUnitUpdateType.UnverifiedEngineHours(this.equipmentUnitId, hours))
            }
            .done { mEngineHoursSaved.value = true }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }

}