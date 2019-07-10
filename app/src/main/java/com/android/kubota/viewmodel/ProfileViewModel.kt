package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.kubota.repository.user.UserRepo

class ProfileViewModel internal constructor(override val userRepo: UserRepo): ViewModel(), LoggedIn by LoggedInDelegate(userRepo) {

    val userName: LiveData<String> = Transformations.map(userRepo.getAccount()) {
        return@map it?.userName ?: ""
    }
}