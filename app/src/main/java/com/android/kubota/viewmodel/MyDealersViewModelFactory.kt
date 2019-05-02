package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyDealersViewModelFactory(private val userRepo: UserRepo, private val dealerPrefsRepo: DealerPreferencesRepo): ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyDealersViewModel(userRepo, dealerPrefsRepo) as T
    }

}