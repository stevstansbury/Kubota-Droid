package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import com.kubota.repository.user.UserRepo

class UserViewModel internal constructor(repo: UserRepo): ViewModel() {
    val user = repo.getAccount()
}