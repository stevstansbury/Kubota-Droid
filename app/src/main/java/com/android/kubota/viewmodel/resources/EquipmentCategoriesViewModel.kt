package com.android.kubota.viewmodel.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.CategoryModelService
import com.kubota.repository.service.CategorySyncResults
import com.kubota.repository.uimodel.KubotaEquipmentCategory
import com.kubota.repository.user.ModelSuggestionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EquipmentCategoriesViewModel(
    private val categoryService: CategoryModelService,
    private val repo: ModelSuggestionRepo
): ViewModel() {

    suspend fun loadCategories(): CategorySyncResults<KubotaEquipmentCategory> {
        return withContext(Dispatchers.IO) {
            categoryService.getCategories()
        }
    }

    fun loadRecentlyViewedModels() = repo.getModelSuggestions()
}

class EquipmentCategoriesViewModelFactory(
    private val categoryService: CategoryModelService,
    private val repo: ModelSuggestionRepo
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentCategoriesViewModel(
            categoryService,
            repo
        ) as T
    }
}