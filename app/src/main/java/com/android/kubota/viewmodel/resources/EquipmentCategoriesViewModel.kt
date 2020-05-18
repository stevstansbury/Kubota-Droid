package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel

class EquipmentCategoriesViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentCategoriesViewModel() as T
    }
}

class EquipmentCategoriesViewModel: ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): EquipmentCategoriesViewModel {
            return ViewModelProvider(owner, EquipmentCategoriesViewModelFactory())
                .get(EquipmentCategoriesViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentCategories = MutableLiveData<List<EquipmentCategory>>(emptyList())
    private val mRecentlyViewModels = MutableLiveData<List<EquipmentModel>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentCategories: LiveData<List<EquipmentCategory>> = mEquipmentCategories
    val recentlyViewedModels: LiveData<List<EquipmentModel>> = mRecentlyViewModels

    fun updateData() {
        mIsLoading.value = true

        val serviceManager = AppProxy.proxy.serviceManager
        whenFulfilled(
            serviceManager.equipmentService.getCategories(),
            serviceManager.browseService.getRecentlyViewedEquipmentModels()
        )
        .done {
            mEquipmentCategories.value = it.first
            mRecentlyViewModels.value = it.second
        }
        .ensure { mIsLoading.value = false }
        .catch { mError.value = it }
    }

}
