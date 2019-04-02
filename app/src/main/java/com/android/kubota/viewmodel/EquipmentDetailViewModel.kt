package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.android.kubota.extensions.toUIModel
import com.kubota.repository.prefs.ModelPreferencesRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EquipmentDetailViewModel(private val modelPrefsRepo: ModelPreferencesRepo): ViewModel()  {
    private val viewModelJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    fun loadModel(model: UIModel): LiveData<UIModel?> = Transformations.map(modelPrefsRepo.getModel(model.id)) {
            return@map it?.toUIModel()
        }

    fun updateSerialNumber(uiModel: UIModel, newSerialNumber: String?)  {
        backgroundTask {
            modelPrefsRepo.updateModel(uiModel.id, newSerialNumber)
        }
    }

    private fun backgroundTask(block: suspend () -> Unit): Job {
        return backgroundScope.launch {
            block()
        }
    }
}