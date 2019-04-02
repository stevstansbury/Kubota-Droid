package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.ModelPreferencesRepo

class EquipmentDetailViewModelFactory(private val modelPrefsRepo: ModelPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EquipmentDetailViewModel(modelPrefsRepo) as T
    }
}