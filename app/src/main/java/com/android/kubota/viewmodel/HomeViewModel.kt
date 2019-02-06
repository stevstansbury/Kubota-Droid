package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.ModelPreferencesRepo

class HomeViewModel(private val modelsRepo: ModelPreferencesRepo,
                    private val dealerRepo: DealerPreferencesRepo): ViewModel() {

    val selectedDealer : LiveData<String?> = Transformations.map(dealerRepo.getSelectedDealer()) {
        return@map if (it != null) it.name else null
    }

    val selectedModel: LiveData<String?> = Transformations.map(modelsRepo.getSelectedModel()) {
        return@map if (it != null) it.manualName else null
    }

}
