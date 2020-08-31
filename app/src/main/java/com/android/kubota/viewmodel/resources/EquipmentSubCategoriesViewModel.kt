package com.android.kubota.viewmodel.resources

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.resources.EquipmentSubCategoryFragment
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel

class EquipmentSubCategoriesViewModelFactory(
    private val parentCategory: String,
    private val viewMode: Int
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentSubCategoriesViewModel(parentCategory, viewMode) as T
    }

}

class EquipmentSubCategoriesViewModel(
    private val parentCategory: String,
    private val viewMode: Int
): ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner, parentCategory: String, viewMode: Int): EquipmentSubCategoriesViewModel {
            return ViewModelProvider(owner, EquipmentSubCategoriesViewModelFactory(parentCategory, viewMode))
                        .get(EquipmentSubCategoriesViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mError = MutableLiveData<Throwable?>(null)
    private val mEquipmentCategories = MutableLiveData<List<EquipmentCategory>>(emptyList())
    private val mEquipmentModels = MutableLiveData<List<EquipmentModel>>(emptyList())

    val isLoading: LiveData<Boolean> = mIsLoading
    val error: LiveData<Throwable?> = mError
    val equipmentCategories: LiveData<List<EquipmentCategory>> = mEquipmentCategories
    val equipmentModels: LiveData<List<EquipmentModel>> = mEquipmentModels

    init {
        this.updateData()
    }

    fun updateData() {
        mIsLoading.value = true

        when( this.viewMode) {
            EquipmentSubCategoryFragment.SUB_CATEGORIES_VIEW_MODE ->
                AppProxy.proxy.serviceManager.equipmentService.getCategories(this.parentCategory)
                    .done { mEquipmentCategories.value = it }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }

            EquipmentSubCategoryFragment.MODELS_VIEW_MODE ->
                AppProxy.proxy.serviceManager.equipmentService.getModels(category = this.parentCategory)
                    .done { mEquipmentModels.value = it }
                    .ensure { mIsLoading.value = false }
                    .catch { mError.value = it }
        }
    }

}
