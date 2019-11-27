package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Equipment
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import java.util.*

class AddEquipmentViewModel(private val equipmentRepo: EquipmentPreferencesRepo) : ViewModel() {

    fun add(modelName: String, serialNumber: String, category: String) {
        // TODO: This should be provided server side
        Utils.backgroundTask {
            equipmentRepo.insertEquipment(
                Equipment(
                    serverId = UUID.randomUUID().toString(),
                    userId = 1,
                    model = modelName,
                    serialNumber = serialNumber,
                    category = category,
                    manualName = "",
                    manualLocation = null,
                    hasGuide = false
                )
            )
        }
    }
}