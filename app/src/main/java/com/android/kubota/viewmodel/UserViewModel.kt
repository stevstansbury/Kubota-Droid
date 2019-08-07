package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.Utils
import com.kubota.repository.service.PreferenceSyncService
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationResult

class UserViewModel internal constructor(private val repo: UserRepo): ViewModel() {

    val user = repo.getAccount()

    fun addUser(context: Context, authenticationResult: AuthenticationResult) {
        AccountPrefs.clearDisclaimerAccepted(context)
        context.stopService(Intent(context, PreferenceSyncService::class.java))
        Utils.backgroundTask {
            repo.logout()
            repo.login(authenticationResult = authenticationResult)
        }
    }

    fun addGuestAccount() = repo.addGuestAccount()

    fun logout(context: Context) {
        AccountPrefs.clearDisclaimerAccepted(context)
        context.stopService(Intent(context, PreferenceSyncService::class.java))
        Utils.backgroundTask {
            repo.logout()
        }
    }
}