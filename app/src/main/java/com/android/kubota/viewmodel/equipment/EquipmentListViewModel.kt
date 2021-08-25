package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.engineHours
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.ui.equipment.EquipmentUnitWrapper
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.android.kubota.viewmodel.notification.UnreadNotificationsViewModel
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.caseInsensitiveSort
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.displayName
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier
import java.util.*

class EquipmentListViewModelFactory : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentListViewModel() as T
    }

}

sealed class EquipmentListDeleteError : Throwable() {
    object CannotDeleteTelematicsEquipment : EquipmentListDeleteError()
}

private fun List<EquipmentUnit>.sortByName(): List<EquipmentUnit> =
    this.caseInsensitiveSort { it.displayName }

class EquipmentListViewModel : UnreadNotificationsViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): EquipmentListViewModel {
            return ViewModelProvider(owner, EquipmentListViewModelFactory())
                .get(EquipmentListViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentList = MutableLiveData<List<EquipmentUnit>>(emptyList())
    private var mIsUpdatingData = false

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentList: LiveData<List<EquipmentUnit>> = mEquipmentList

    fun clearError() {
        mError.postValue(null)
    }

    fun updateData(delegate: AuthDelegate?) {
        if (this.mIsUpdatingData) return

        mError.value = null
        when (AppProxy.proxy.accountManager.isAuthenticated.value) {
            true -> {
                this.mIsUpdatingData = true
                this.mIsLoading.value = true
                loadUnreadNotification(delegate)
                AuthPromise(delegate)
                    .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipment() }
                    .done {
                        mEquipmentList.value = it.sortByName()
                    }
                    .ensure {
                        mIsLoading.value = false
                        mIsUpdatingData = false
                    }
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
                    model = unit.nickName ?: unit.model,
                    engineHours = unit.engineHours
                )
                AppProxy.proxy.serviceManager.userPreferenceService.addEquipmentUnit(request = request)
            }
            .done { mEquipmentList.value = it.sortByName() }
            .catch { mError.value = it }
    }

    fun deleteEquipmentUnit(delegate: AuthDelegate?, unitId: UUID) {
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnit(id = unitId) }
            .done { mEquipmentList.value = it.sortByName() }
            .catch {
                when (it) {
                    is KubotaServiceError.Forbidden ->
                        mError.value = EquipmentListDeleteError.CannotDeleteTelematicsEquipment
                    else ->
                        mError.value = it
                }
            }
    }

    fun deleteEquipmentUnit(delegate: AuthDelegate?, unit: EquipmentUnit) {
        deleteEquipmentUnit(delegate = delegate, unitId = unit.id)
    }

    fun deleteEquipmentUnits(delegate: AuthDelegate?, units: List<EquipmentUnit>) {
        AuthPromise(delegate)
            .then { AppProxy.proxy.serviceManager.userPreferenceService.removeEquipmentUnits(units = units) }
            .done { mEquipmentList.value = it.sortByName() }
            .catch {
                when (it) {
                    is KubotaServiceError.Forbidden ->
                        mError.value = EquipmentListDeleteError.CannotDeleteTelematicsEquipment
                    else ->
                        mError.value = it
                }
            }
    }

    fun createDeleteAction(delegate: AuthDelegate?, unit: EquipmentUnitWrapper): UndoAction {
        return object : UndoAction {
            override fun commit() {
                unit.equipment?.let {
                    deleteEquipmentUnit(delegate, it)
                }
            }

            override fun undo() {
                unit.equipment?.let {
                    addEquipmentUnit(delegate, it)
                }
            }
        }
    }

    fun createMultiDeleteAction(delegate: AuthDelegate?, units: List<EquipmentUnit>): UndoAction {
        return object : UndoAction {
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
                                model = unit.nickName ?: unit.model,
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
