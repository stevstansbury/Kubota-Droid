package com.android.kubota.viewmodel.equipment

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentCategory
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

class EquipmentUnitViewModel(
    unit: EquipmentUnit
) : ViewModel() {

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
    private val mCompatibleAttachments = MutableLiveData<List<Map<EquipmentCategory, List<Any>>>>()

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentUnit: LiveData<EquipmentUnit?> = mEquipmentUnit
    val unitUpdated: LiveData<Boolean> = mUnitUpdated
    val compatibleAttachments: LiveData<List<Map<EquipmentCategory, List<Any>>>> = mCompatibleAttachments

    init {
        loadCompatibleAttachments()
    }

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
            AppProxy.proxy.serviceManager.equipmentService.getAttachmentCategories(model = unit.model)
                .done { mCompatibleAttachments.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .catch { mError.postValue(it) }
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