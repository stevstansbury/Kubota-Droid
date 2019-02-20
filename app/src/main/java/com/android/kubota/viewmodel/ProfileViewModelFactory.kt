package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kubota.repository.user.UserRepo

class ProfileViewModelFactory(private val repository: UserRepo): ViewModelProvider.NewInstanceFactory()  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ProfileViewModel(repository) as T
    }
}