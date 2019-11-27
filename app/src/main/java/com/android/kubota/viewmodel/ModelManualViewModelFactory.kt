package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class ModelManualViewModelFactory(private val equipmentPrefsRepo: EquipmentPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ModelManualViewModel(equipmentPrefsRepo) as T
    }

}