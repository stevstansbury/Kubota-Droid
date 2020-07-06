package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.engineHours
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.util.*

class EquipmentListViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentListViewModel() as T
    }

}

class EquipmentListViewModel: ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): EquipmentListViewModel {
            return ViewModelProvider(owner, EquipmentListViewModelFactory())
                        .get(EquipmentListViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentList = MutableLiveData<List<EquipmentUnit>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentList: LiveData<List<EquipmentUnit>> = mEquipmentList

    fun updateData(delegate: AuthDelegate?) {
        when (AppProxy.proxy.accountManager.isAuthenticated.value ) {
            true -> {
                this.mIsLoading.value = true
                AuthPromise(delegate)
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipment() }
                    .done {
                        mEquipmentList.value = it
                    }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
            }
            else -> {
                mEquipmentList.value = emptyList()
            }
        }
    }

    fun addEquipmentUnit(delegate: AuthDelegate?, unit: EquipmentUnit) {
        AuthPromise(delegate)
            .then {
                val request = AddEquipmentUnitRequest(
                    identifierType = EquipmentUnitIdentifier.valueOf(unit.identifierType),
                    pinOrSerial = unit.pinOrSerial,
                    model = unit.model,
                    nickName = unit.nickName,
                    engineHours = unit.engineHours
                )
                AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
            }
            .done { mEquipmentList.value = it }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnit(delegate: AuthDelegate?, unitId: UUID) {
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnit(id = unitId) }
            .done { mEquipmentList.value = it }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnit(delegate: AuthDelegate?, unit: EquipmentUnit) {
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnit(id = unit.id) }
            .done { mEquipmentList.value = it }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnits(delegate: AuthDelegate?, units: List<EquipmentUnit>) {
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnits(units = units) }
            .done { mEquipmentList.value = it }
            .catch { mError.value = it }
    }

    fun createDeleteAction(delegate: AuthDelegate?, unit: EquipmentUnit): UndoAction {
        return object : UndoAction {
            override fun commit() {
                deleteEquipmentUnit(delegate, unit)
            }

            override fun undo() {
                addEquipmentUnit(delegate, unit)
            }
        }
    }

    fun createMultiDeleteAction(delegate: AuthDelegate?, units: List<EquipmentUnit>): UndoAction {
        return object : UndoAction{
            override fun commit() {
                // Multi-delete is a problem with remote service calls
                deleteEquipmentUnits(delegate, units)
            }

            override fun undo() {
                AuthPromise(delegate)
                    .then {
                        val tasks = units.map { unit ->
                            val request = AddEquipmentUnitRequest(
                                identifierType = EquipmentUnitIdentifier.valueOf(unit.identifierType),
                                pinOrSerial = unit.pinOrSerial,
                                model = unit.model,
                                nickName = unit.nickName,
                                engineHours = unit.engineHours
                            )
                            return@map AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
                        }
                        whenFulfilled(tasks)
                    }
                    .catch { mError.value = it }
            }
        }
    }

}
