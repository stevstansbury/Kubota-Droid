package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.CategoryModelService

class SearchEquipmentViewModelFactory(private val categoryService: CategoryModelService): ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SearchEquipmentViewModel(categoryService) as T
    }
}

class SearchDealersViewFactory: ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SearchDealersViewModel() as T
    }
}