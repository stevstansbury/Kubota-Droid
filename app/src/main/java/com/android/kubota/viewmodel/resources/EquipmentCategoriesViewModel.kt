package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.RecentViewedItem

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
    private val mRecentViewItems = MutableLiveData<List<RecentViewedItem>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentCategories: LiveData<List<EquipmentCategory>> = mEquipmentCategories
    val recentViewedItems: LiveData<List<RecentViewedItem>> = mRecentViewItems

    fun updateData() {
        mIsLoading.value = true

        val serviceManager = AppProxy.proxy.serviceManager
        whenFulfilled(
            serviceManager.equipmentService.getCategories(),
            serviceManager.browseService.getRecentViewedItems()
        )
        .done { pair ->
            mEquipmentCategories.value = pair.first.sortedBy { it.category }
            mRecentViewItems.value = pair.second
        }
        .ensure { mIsLoading.value = false }
        .catch { mError.value = it }
    }

    fun updateRecentViewed() {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.browseService.getRecentViewedItems()
            .done { mRecentViewItems.value = it }
            .ensure { mIsLoading.value = false }
            .catch { mError.value = it }
    }

}
