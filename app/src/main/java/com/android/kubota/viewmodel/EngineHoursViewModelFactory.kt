package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import java.util.*

class EngineHoursViewModelFactory(
    private val equipmentId: UUID
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>) =
        EngineHoursViewModel(equipmentId) as T

}