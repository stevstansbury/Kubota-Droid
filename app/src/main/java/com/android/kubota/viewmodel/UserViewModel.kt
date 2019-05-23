package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.content.Context
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.Utils
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationResult

class UserViewModel internal constructor(private val repo: UserRepo): ViewModel() {

    val user = repo.getAccount()

    fun addUser(context: Context, authenticationResult: AuthenticationResult) {
        AccountPrefs.clearDisclaimerAccepted(context)
        Utils.backgroundTask {
            repo.logout()
            repo.login(authenticationResult = authenticationResult)
        }
    }

    fun addGuestAccount() = repo.addGuestAccount()

    fun logout(context: Context) {
        AccountPrefs.clearDisclaimerAccepted(context)
        Utils.backgroundTask {
            repo.logout()
        }
    }
}