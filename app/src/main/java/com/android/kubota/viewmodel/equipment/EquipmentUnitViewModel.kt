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
import java.lang.ref.WeakReference
import java.util.*

class EquipmentUnitViewModelFactory(
    private val equipmentUnitId: UUID,
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnitId, signInHandler ) as T
    }

}

class EquipmentUnitViewModel(
    private val equipmentUnitId: UUID,
    private val signInHandler: WeakReference<SignInHandler>?
) : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner,
                     equipmentUnitId: UUID,
                     signInHandler: WeakReference<SignInHandler>?
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnitId, signInHandler))
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
        this.updateEquipmentUnit()
    }

    fun updateEquipmentUnit() {
        when (AppProxy.proxy.accountManager.isAuthenticated.value ) {
            true -> {
                this.mIsLoading.value = true
                AuthPromise()
                    .onSignIn { signIn() }
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

    private fun signIn(): Promise<Unit> {
        return signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }

}