package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.engineHours
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.util.*


class AddEquipmentViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddEquipmentViewModel() as T
    }
}

class AddEquipmentViewModel: ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): AddEquipmentViewModel {
            return ViewModelProvider(owner)
                .get(AddEquipmentViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mNewEquipmentId = MutableLiveData<UUID>()

    val newEquipmentId: LiveData<UUID> = mNewEquipmentId
    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError

    fun addEquipmentUnit(delegate: AuthDelegate, unit: EquipmentUnit) {
        mIsLoading.postValue(true)
        mError.postValue(null)
        AuthPromise(delegate)
            .then {
                val request = AddEquipmentUnitRequest(
                    identifierType = EquipmentUnitIdentifier.valueOf(unit.identifierType),
                    pinOrSerial = unit.pinOrSerial,
                    model = unit.nickName ?: unit.model,
                    engineHours = unit.engineHours
                )
                AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
            }
            .ensure { mIsLoading.postValue(false) }
            .map { it.first { it.pinOrSerial == unit.pinOrSerial }.id }
            .done { mNewEquipmentId.postValue(it) }
            .catch { mError.value = it }
    }

}
