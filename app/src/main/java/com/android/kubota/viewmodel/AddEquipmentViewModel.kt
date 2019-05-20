package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Model
import com.kubota.repository.prefs.ModelPreferencesRepo
import java.util.*

class AddEquipmentViewModel(private val modelRepo: ModelPreferencesRepo) : ViewModel() {

    fun add(modelName: String, serialNumber: String, category: String) {
        // TODO: This should be provided server side
        Utils.backgroundTask {
            modelRepo.insertModel(
                Model(
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