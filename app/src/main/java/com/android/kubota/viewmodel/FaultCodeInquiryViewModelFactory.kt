package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class FaultCodeInquiryViewModelFactory (
    private val equipmentRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>) =
        FaultCodeInquiryViewModel(equipmentRepo, equipmentId) as T

}