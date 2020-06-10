package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.SignInHandler
import com.inmotionsoftware.promisekt.*
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.lang.ref.WeakReference
import java.util.*


class AddEquipmentViewModelFactory(
    private val signInHandler: WeakReference<SignInHandler>?
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddEquipmentViewModel(signInHandler) as T
    }
}

class AddEquipmentViewModel(
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner, signInHandler: WeakReference<SignInHandler>?): AddEquipmentViewModel {
            return ViewModelProvider(owner, AddEquipmentViewModelFactory(signInHandler))
                .get(AddEquipmentViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mNewEquipmentId = MutableLiveData<UUID>()

    val newEquipmentId: LiveData<UUID> = mNewEquipmentId
    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError

    fun addEquipmentUnit(unit: EquipmentUnit) {
        mIsLoading.postValue(true)
        mError.postValue(null)
        AuthPromise()
            .onSignIn { signIn() }
            .then {
                val request = AddEquipmentUnitRequest(
                    identifierType = EquipmentUnitIdentifier.valueOf(unit.identifierType),
                    pinOrSerial = unit.pinOrSerial,
                    model = unit.model,
                    nickName = unit.nickName,
                    engineHours = unit.engineHours ?: 0.0
                )
                AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
            }
            .ensure { mIsLoading.postValue(false) }
            .map { it.equipment!!.first { it.pinOrSerial == unit.pinOrSerial }.id }
            .done { mNewEquipmentId.postValue(it) }
            .catch { mError.value = it }
    }

    private fun signIn(): Promise<Unit> {
        return signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }
}