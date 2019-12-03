package com.android.kubota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyDealersViewModelFactory(private val userRepo: UserRepo, private val dealerPrefsRepo: DealerPreferencesRepo): ViewModelProvider.NewInstanceFactory()  {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyDealersViewModel(userRepo, dealerPrefsRepo) as T
    }

}