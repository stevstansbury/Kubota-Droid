package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class EquipmentDetailViewModelFactory(
    private val equipmentPrefsRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentDetailViewModel(equipmentPrefsRepo, equipmentId) as T
    }
}