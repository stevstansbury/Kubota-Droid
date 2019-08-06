package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.ModelPreferencesRepo

class ModelManualViewModelFactory(private val modelPrefsRepo: ModelPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ModelManualViewModel(modelPrefsRepo) as T
    }

}