package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.extensions.toRecentViewedItem
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.RecentViewedItem
import java.util.*

class EquipmentModelViewModelFactory(
    private val model: String
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentModelViewModel(model) as T
    }
}

class EquipmentModelViewModel(
    private val model: String
): ViewModel() {

    companion object {
        private const val recentViewedItemMaxCount = 5

        fun instance(owner: ViewModelStoreOwner, model: String): EquipmentModelViewModel {
            return ViewModelProvider(owner, EquipmentModelViewModelFactory(model))
                        .get(EquipmentModelViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentModel = MutableLiveData<EquipmentModel?>(null)

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentModel: LiveData<EquipmentModel?> = mEquipmentModel

    init {
        this.updateData()
    }

    fun updateData() {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.equipmentService.getModel(this.model)
                .done { mEquipmentModel.value = it }
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
    }

    fun saveRecentlyViewed(model: EquipmentModel) {
        mIsLoading.value = true
        AppProxy.proxy.serviceManager.browseService.addRecentViewedItem(item = model.toRecentViewedItem(), limitTo = recentViewedItemMaxCount)
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
    }

}
