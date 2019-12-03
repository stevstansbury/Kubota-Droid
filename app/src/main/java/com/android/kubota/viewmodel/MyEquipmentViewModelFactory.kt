package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModelFactory(private val userRepo: UserRepo, private val equipmentPrefsRepo: EquipmentPreferencesRepo): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyEquipmentViewModel(userRepo, equipmentPrefsRepo) as T
    }
}