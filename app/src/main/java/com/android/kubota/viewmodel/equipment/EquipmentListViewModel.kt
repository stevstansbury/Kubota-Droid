package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.AuthPromise
import com.android.kubota.utility.SignInHandler
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.lang.ref.WeakReference

class EquipmentListViewModelFactory(
    private val signInHandler: WeakReference<SignInHandler>?
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentListViewModel(signInHandler) as T
    }

}

class EquipmentListViewModel(
    private val signInHandler: WeakReference<SignInHandler>?
) : ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner, signInHandler: WeakReference<SignInHandler>?): EquipmentListViewModel {
            return ViewModelProvider(owner, EquipmentListViewModelFactory(signInHandler))
                        .get(EquipmentListViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentList = MutableLiveData<List<EquipmentUnit>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentList: LiveData<List<EquipmentUnit>> = mEquipmentList

    fun updateData() {
        when (AppProxy.proxy.accountManager.isAuthenticated.value ) {
            true -> {
                this.mIsLoading.value = true
                AuthPromise()
                    .onSignIn { signIn() }
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getUserPreference() }
                    .done {
                        mEquipmentList.value = it.equipment ?: emptyList()
                    }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> {
                mEquipmentList.value = emptyList()
            }
        }
    }

    fun addEquipmentUnit(unit: EquipmentUnit) {
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
            .done { mEquipmentList.value = it.equipment ?: emptyList() }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnit(unit: EquipmentUnit) {
        AuthPromise()
            .onSignIn { signIn() }
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnit(id = unit.id) }
            .done { mEquipmentList.value = it.equipment ?: emptyList() }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnits(units: List<EquipmentUnit>) {
        AuthPromise()
            .onSignIn { signIn() }
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnits(units = units) }
            .done { mEquipmentList.value = it.equipment ?: emptyList() }
            .catch { mError.value = it }
    }

    fun createDeleteAction(unit: EquipmentUnit): UndoAction {
        return object : UndoAction {
            override fun commit() {
                deleteEquipmentUnit(unit)
            }

            override fun undo() {
                addEquipmentUnit(unit)
            }
        }
    }

    fun createMultiDeleteAction(units: List<EquipmentUnit>): UndoAction {
        return object : UndoAction{
            override fun commit() {
                // Multi-delete is a problem with remote service calls
                deleteEquipmentUnits(units)
            }

            override fun undo() {
                AuthPromise()
                    .onSignIn { signIn() }
                    .then {
                        val tasks = units.map { unit ->
                            val request = AddEquipmentUnitRequest(
                                identifierType = EquipmentUnitIdentifier.valueOf(unit.identifierType),
                                pinOrSerial = unit.pinOrSerial,
                                model = unit.model,
                                nickName = unit.nickName,
                                engineHours = unit.engineHours ?: 0.0
                            )
                            return@map AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
                        }
                        whenFulfilled(tasks)
                    }
                    .catch { mError.value = it }
            }
        }
    }

    private fun signIn(): Promise<Unit> {
        return signInHandler?.get()?.let { it() } ?: Promise.value(Unit)
    }

}
