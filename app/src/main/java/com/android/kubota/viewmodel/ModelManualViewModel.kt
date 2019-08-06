package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.kubota.repository.prefs.ModelPreferencesRepo

class ModelManualViewModel(private val modelPrefsRepo: ModelPreferencesRepo): ViewModel(){

    fun getModelManualLocation(modelId: Int): LiveData<String?> = Transformations.map(modelPrefsRepo.getModel(modelId)) {
        return@map it?.manualLocation
    }

}