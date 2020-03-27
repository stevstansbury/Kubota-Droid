package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.uimodel.Equipment

class EquipmentDetailViewModel(
    private val equipmentPrefsRepo: EquipmentPreferencesRepo,
    equipmentId: Int
): ViewModel()  {
    val liveData: LiveData<Equipment?> = equipmentPrefsRepo.getSavedEquipment(equipmentId)

    fun updateSerialNumber(equipmentId: Int, newSerialNumber: String?)  {
        Utils.backgroundTask {
            equipmentPrefsRepo.updateEquipmentSerialPin(equipmentId, newSerialNumber)
        }
    }

    fun updateNickName(equipmentId: Int, newSerialNumber: String?) {
        Utils.backgroundTask {
            equipmentPrefsRepo.updateEquipmentNickName(equipmentId, newSerialNumber)
        }
    }
}