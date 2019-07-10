package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
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