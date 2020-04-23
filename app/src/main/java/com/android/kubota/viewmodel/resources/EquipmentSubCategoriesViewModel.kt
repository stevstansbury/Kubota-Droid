package com.android.kubota.viewmodel.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.EquipmentCategory
import com.kubota.repository.uimodel.KubotaModel
import com.kubota.repository.uimodel.KubotaModelSubCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EquipmentSubCategoriesViewModel(
    private val categoryService: CategoryModelService
): ViewModel() {

    suspend fun loadSubCategories(category: EquipmentCategory): CategorySyncResults<KubotaModelSubCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getSubCategories(category)
        }
    }

    suspend fun loadModels(category: EquipmentCategory): CategorySyncResults<KubotaModel> {
        return withContext(Dispatchers.IO) {
            categoryService.getModels(category)
        }
    }

    suspend fun loadModels(subCategory: KubotaModelSubCategory): CategorySyncResults<KubotaModel> {
        return withContext(Dispatchers.IO) {
            categoryService.getModels(subCategory)
        }
    }
}

class EquipmentSubCategoriesViewModelFactory(
    private val categoryService: CategoryModelService
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentSubCategoriesViewModel(
            categoryService
        ) as T
    }
}