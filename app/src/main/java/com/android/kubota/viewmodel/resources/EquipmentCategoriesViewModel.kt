package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.viewmodel.notification.UnreadNotificationsViewModel
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.api.caseInsensitiveSort
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.RecentViewedItem

class EquipmentCategoriesViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentCategoriesViewModel() as T
    }
}

class EquipmentCategoriesViewModel: UnreadNotificationsViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): EquipmentCategoriesViewModel {
            return ViewModelProvider(owner, EquipmentCategoriesViewModelFactory())
                .get(EquipmentCategoriesViewModel::class.java)
        }
    }

    private val mLoading = MutableLiveData(0)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentCategories = MutableLiveData<List<EquipmentCategory>>(emptyList())
    private val mRecentViewItems = MutableLiveData<List<RecentViewedItem>>(emptyList())

    val loading: LiveData<Int> = mLoading
    val error: LiveData<Throwable?> = mError
    val equipmentCategories: LiveData<List<EquipmentCategory>> = mEquipmentCategories
    val recentViewedItems: LiveData<List<RecentViewedItem>> = mRecentViewItems

    fun updateData() {
        this.postLoading()

        val serviceManager = AppProxy.proxy.serviceManager
        whenFulfilled(
            serviceManager.equipmentService.getCategories(),
            serviceManager.browseService.getRecentViewedItems()
        )
        .done { pair ->
            mEquipmentCategories.postValue(pair.first.caseInsensitiveSort { it.category })
            mRecentViewItems.postValue(pair.second)
        }
        .ensure { this.postFinished() }
        .catch { mError.postValue(it) }
    }

    fun updateRecentViewed() {
        this.postLoading()
        AppProxy.proxy.serviceManager.browseService.getRecentViewedItems()
            .done { mRecentViewItems.postValue(it) }
            .ensure { this.postFinished() }
            .catch { mError.postValue(it) }
    }

    private fun postLoading() {
        mLoading.postValue((mLoading.value ?: 0) + 1)
    }

    private fun postFinished() {
        mLoading.postValue((mLoading.value ?: 1) - 1)
    }

}
