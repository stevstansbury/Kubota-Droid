package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.DealerPreferencesRepo

class DealerDetailViewModelFactory(private val dealerPreferencesRepo: DealerPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DealerDetailViewModel(dealerPreferencesRepo) as T
    }
}