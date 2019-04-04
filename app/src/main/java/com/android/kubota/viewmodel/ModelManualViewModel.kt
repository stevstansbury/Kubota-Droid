package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.kubota.repository.prefs.ModelPreferencesRepo

class ModelManualViewModel(private val modelPrefsRepo: ModelPreferencesRepo): ViewModel(){

    fun getModelManualLocation(modelId: Int): LiveData<String?> = Transformations.map(modelPrefsRepo.getModel(modelId)) {
        return@map it?.manualLocation
    }

}