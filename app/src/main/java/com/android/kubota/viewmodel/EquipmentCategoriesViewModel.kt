package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.EquipmentCategory
import com.kubota.repository.uimodel.KubotaEquipmentCategory
import com.kubota.repository.uimodel.KubotaModelSubCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EquipmentCategoriesViewModel(
    private val categoryService: CategoryModelService
): ViewModel() {

    suspend fun loadCategories(): CategorySyncResults<KubotaEquipmentCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getCategories()
        }
    }

    suspend fun loadSubCategories(category: EquipmentCategory): CategorySyncResults<KubotaModelSubCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getSubCategories(category)
        }
    }

    suspend fun loadModels(category: EquipmentCategory): CategorySyncResults<KubotaModelSubCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getModels(category)
        }
    }

    suspend fun loadModels(subCategory: KubotaModelSubCategory): CategorySyncResults<KubotaModelSubCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getModels(subCategory)
        }
    }
}

class EquipmentCategoriesViewModelFactory(
    private val categoryService: CategoryModelService
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentCategoriesViewModel(categoryService) as T
    }
}