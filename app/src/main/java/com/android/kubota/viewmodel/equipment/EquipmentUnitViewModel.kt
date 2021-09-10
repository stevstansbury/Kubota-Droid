package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.EquipmentUnitUpdate

class EquipmentUnitViewModelFactory(
    private val equipmentUnit: EquipmentUnit
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentUnitViewModel(equipmentUnit) as T
    }
}

class EquipmentUnitViewModel(unit: EquipmentUnit) : ViewModel() {

    companion object {
        fun instance(
            owner: ViewModelStoreOwner,
            equipmentUnit: EquipmentUnit
        ): EquipmentUnitViewModel {
            return ViewModelProvider(owner, EquipmentUnitViewModelFactory(equipmentUnit))
                .get(EquipmentUnitViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentUnit = MutableLiveData(unit)
    private val mUnitUpdated = MutableLiveData(false)
    private val mCompatibleAttachments = MutableLiveData<List<EquipmentModelTree>>()
    private val mCompatibleMachines = MutableLiveData<List<EquipmentModel>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentUnit: LiveData<EquipmentUnit?> = mEquipmentUnit
    val unitUpdated: LiveData<Boolean> = mUnitUpdated
    val compatibleAttachments: LiveData<List<EquipmentModelTree>> = mCompatibleAttachments
    val compatibleMachines = mCompatibleMachines

    fun reload(delegate: AuthDelegate?) {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)
            AuthPromise(delegate = delegate)
                .then { AppProxy.proxy.serviceManager.userPreferenceService.getEquipmentUnit(unit.id) }
                .done { mEquipmentUnit.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .done { loadCompatibleAttachments() }
                .catch { mError.postValue(it) }
        }
    }

    fun loadCompatibleAttachments() {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)

            val equipmentService = AppProxy.proxy.serviceManager.equipmentService
            equipmentService.getModel(unit.model)
                .thenMap { equipmentModel ->
                    val model = equipmentModel
                        ?: throw IllegalStateException("exists as equipment unit, must exist as a model")

                    when (model.compatibleAttachments.isEmpty()) {
                        true -> Promise.value(emptyList())
                        false -> {
                            val filter = EquipmentTreeFilter.AttachmentsCompatibleWith(unit.model)
                            equipmentService.getEquipmentTree(listOf(filter))
                        }
                    }
                }
                .done { mCompatibleAttachments.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .catch { mError.postValue(it) }
        }
    }

    fun loadCompatibleMachines() {
        mEquipmentUnit.value?.let { unit ->
            mIsLoading.postValue(true)

            AppProxy.proxy.serviceManager.equipmentService.getCompatibleMachines(unit.model)
                .done { mCompatibleMachines.postValue(it) }
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
        }
    }

    fun updateEquipmentUnit(delegate: AuthDelegate?, nickName: String?, engineHours: Double?) {
        val equipmentUnit = this.equipmentUnit.value ?: return
        this.mUnitUpdated.postValue(false)
        this.mIsLoading.postValue(true)
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateEquipmentUnit(
                        EquipmentUnitUpdate(
                            equipmentUnit.id,
                            nickName = nickName?.trim(),
                            engineHours = engineHours?.let { Math.abs(it) })
                    )
            }
            .done { equipment ->
                equipment.firstOrNull {
                    it.id == equipmentUnit.id
                        && it.model == equipmentUnit.model
                        && it.pinOrSerial == equipmentUnit.pinOrSerial
                }?.let {
                    mEquipmentUnit.postValue(it)
                    mUnitUpdated.postValue(true)
                }
            }
            .ensure { this.mIsLoading.value = false }
            .catch { this.mError.value = it }
    }
}