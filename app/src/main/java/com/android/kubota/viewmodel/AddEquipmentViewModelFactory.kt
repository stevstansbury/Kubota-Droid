package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class AddEquipmentViewModelFactory(private val equipmentRepo: EquipmentPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddEquipmentViewModel(equipmentRepo) as T
    }
}
