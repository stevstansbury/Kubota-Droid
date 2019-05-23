package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.Utils
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationResult

class ProfileViewModel internal constructor(override val userRepo: UserRepo): ViewModel(), LoggedIn by LoggedInDelegate(userRepo) {

    val userName: LiveData<String> = Transformations.map(userRepo.getAccount()) {
        return@map it?.userName ?: ""
    }

    fun addUser(context: Context, authenticationResult: AuthenticationResult) {
        AccountPrefs.clearDisclaimerAccepted(context)
        Utils.backgroundTask {
            userRepo.logout()
            userRepo.login(authenticationResult = authenticationResult)
        }
    }

    fun logout(context: Context) {
        AccountPrefs.clearDisclaimerAccepted(context)
        Utils.backgroundTask {
            userRepo.logout()
        }
    }
}