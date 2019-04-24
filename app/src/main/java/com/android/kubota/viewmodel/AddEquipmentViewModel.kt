package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import com.android.kubota.extensions.backgroundTask
import com.kubota.repository.data.Model
import com.kubota.repository.prefs.ModelPreferencesRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*

class AddEquipmentViewModel(private val modelRepo: ModelPreferencesRepo) : ViewModel() {
    private val viewModelJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    fun add(modelName: String, serialNumber: String, category: String) {
        // TODO: This should be provided server side
        backgroundScope.backgroundTask {
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