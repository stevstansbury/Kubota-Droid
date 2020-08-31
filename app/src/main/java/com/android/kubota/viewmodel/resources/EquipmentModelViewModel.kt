package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.toRecentViewedItem
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentModel

class EquipmentModelViewModelFactory
    : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentModelViewModel() as T
    }
}

class EquipmentModelViewModel: ViewModel() {

    companion object {
        private const val recentViewedItemMaxCount = 5

        fun instance(owner: ViewModelStoreOwner): EquipmentModelViewModel {
            return ViewModelProvider(owner, EquipmentModelViewModelFactory())
                        .get(EquipmentModelViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError

    fun saveRecentlyViewed(model: EquipmentModel) {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.browseService.addRecentViewedItem(item = model.toRecentViewedItem(), limitTo = recentViewedItemMaxCount)
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
    }

}
