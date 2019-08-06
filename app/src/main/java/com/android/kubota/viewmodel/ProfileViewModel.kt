package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.kubota.repository.user.UserRepo

class ProfileViewModel internal constructor(override val userRepo: UserRepo): ViewModel(), LoggedIn by LoggedInDelegate(userRepo) {

    val userName: LiveData<String> = Transformations.map(userRepo.getAccount()) {
        return@map it?.userName ?: ""
    }
}