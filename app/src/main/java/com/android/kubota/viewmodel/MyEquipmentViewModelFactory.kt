package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModelFactory(private val userRepo: UserRepo, private val modelPrefsRepo: ModelPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyEquipmentViewModel(userRepo, modelPrefsRepo) as T
    }
}