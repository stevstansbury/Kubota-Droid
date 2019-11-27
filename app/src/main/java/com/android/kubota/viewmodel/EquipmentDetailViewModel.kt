package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.android.kubota.extensions.toUIEquipment
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class EquipmentDetailViewModel(private val equipmentPrefsRepo: EquipmentPreferencesRepo): ViewModel()  {

    fun loadModel(equipment: UIEquipment): LiveData<UIEquipment?> = Transformations.map(equipmentPrefsRepo.getEquipment(equipment.id)) {
            return@map it?.toUIEquipment()
        }

    fun updateSerialNumber(uiEquipment: UIEquipment, newSerialNumber: String?)  {
        Utils.backgroundTask {
            equipmentPrefsRepo.updateEquipment(uiEquipment.id, newSerialNumber)
        }
    }
}