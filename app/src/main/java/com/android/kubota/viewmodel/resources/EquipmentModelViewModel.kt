package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.toRecentViewedItem
import com.android.kubota.viewmodel.equipment.EquipmentTreeFilter
import com.android.kubota.viewmodel.equipment.getEquipmentTree
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentModelTree
import com.kubota.service.domain.EquipmentModel

class EquipmentModelViewModelFactory
    : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentModelViewModel() as T
    }
}

class EquipmentModelViewModel : ViewModel() {

    companion object {
        private const val recentViewedItemMaxCount = 5

        fun instance(owner: ViewModelStoreOwner): EquipmentModelViewModel {
            return ViewModelProvider(owner, EquipmentModelViewModelFactory())
                .get(EquipmentModelViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mCompatibleAttachments = MutableLiveData<List<EquipmentModelTree>>()
    private val mCompatibleMachines = MutableLiveData<List<EquipmentModel>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val compatibleAttachments: LiveData<List<EquipmentModelTree>> = mCompatibleAttachments
    val compatibleMachines = mCompatibleMachines

    fun saveRecentlyViewed(model: EquipmentModel) {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.browseService.addRecentViewedItem(
            item = model.toRecentViewedItem(),
            limitTo = recentViewedItemMaxCount
        )
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

    fun loadCompatibleMachines(model: EquipmentModel) {
        AppProxy.proxy.serviceManager.equipmentService.getCompatibleMachines(model.model)
            .done { mCompatibleMachines.postValue(it) }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

    fun loadCompatibleAttachments(model: EquipmentModel) {
        if (model.compatibleAttachments.isNotEmpty()) {
            mIsLoading.postValue(true)

            val filter = EquipmentTreeFilter.AttachmentsCompatibleWith(model.model)
            AppProxy.proxy.serviceManager.equipmentService.getEquipmentTree(listOf(filter))
                .done { mCompatibleAttachments.postValue(it) }
                .ensure { mIsLoading.postValue(false) }
                .catch { mError.postValue(it) }
        }
    }
}
