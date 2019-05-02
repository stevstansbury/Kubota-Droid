package com.android.kubota.viewmodel

import android.arch.lifecycle.ViewModel
import android.content.Context
import com.android.kubota.utility.AccountPrefs
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserViewModel internal constructor(private val repo: UserRepo): ViewModel() {
    private val viewModelJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    val user = repo.getAccount()

    fun addUser(context: Context, authenticationResult: AuthenticationResult) {
        AccountPrefs.clearDisclaimerAccepted(context)
        backgroundTask {
            repo.logout()
            repo.login(authenticationResult = authenticationResult)
        }
    }

    fun addGuestAccount() {
        if (user.value == null) {
            repo.addGuestAccount()
        }
    }

    private fun backgroundTask(block: suspend () -> Unit): Job {
        return backgroundScope.launch {
            block()
        }
    }
}