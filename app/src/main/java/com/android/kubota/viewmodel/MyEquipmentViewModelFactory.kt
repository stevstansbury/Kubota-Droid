package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModelFactory(): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyEquipmentViewModel() as T
    }
}