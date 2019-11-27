package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class ModelManualViewModel(private val equipmentPrefsRepo: EquipmentPreferencesRepo): ViewModel(){

    fun getModelManualLocation(modelId: Int): LiveData<String?> = Transformations.map(equipmentPrefsRepo.getEquipment(modelId)) {
        return@map it?.manualLocation
    }

}