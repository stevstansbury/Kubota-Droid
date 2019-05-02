package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.service.CategoryModelService

class ChooseEquipmentViewModelFactory(private val categoryService: CategoryModelService): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ChooseEquipmentViewModel(categoryService) as T
    }
}
