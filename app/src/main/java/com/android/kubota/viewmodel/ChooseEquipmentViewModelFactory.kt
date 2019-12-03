package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.CategoryModelService

class ChooseEquipmentViewModelFactory(private val categoryService: CategoryModelService): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChooseEquipmentViewModel(categoryService) as T
    }
}
