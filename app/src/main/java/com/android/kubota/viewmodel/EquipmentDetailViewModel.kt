package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.android.kubota.extensions.toUIModel
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.ModelPreferencesRepo

class EquipmentDetailViewModel(private val modelPrefsRepo: ModelPreferencesRepo): ViewModel()  {

    fun loadModel(model: UIModel): LiveData<UIModel?> = Transformations.map(modelPrefsRepo.getModel(model.id)) {
            return@map it?.toUIModel()
        }

    fun updateSerialNumber(uiModel: UIModel, newSerialNumber: String?)  {
        Utils.backgroundTask {
            modelPrefsRepo.updateModel(uiModel.id, newSerialNumber)
        }
    }
}