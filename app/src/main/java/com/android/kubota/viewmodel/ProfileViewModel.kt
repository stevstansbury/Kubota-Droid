package com.android.kubota.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//TODO Determine if we can inherit from UserViewModel since a lot of the methods seem the same
class ProfileViewModel internal constructor(private val repo: UserRepo): ViewModel() {
    private val viewModelJob = Job()
    private val backgroundScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    val userName: LiveData<String> = Transformations.map(repo.getAccount()) {
        return@map it?.userName ?: ""
    }

    val isUserLoggedIn: LiveData<Boolean> = Transformations.map(repo.getAccount()) {
        return@map it?.isGuest()?.not() ?: true
    }

    fun addUser(authenticationResult: AuthenticationResult) {
        backgroundTask {
            repo.logout()
            repo.login(authenticationResult = authenticationResult)
        }
    }

    fun logout() {
        backgroundTask {
            repo.logout()
        }
    }

    private fun backgroundTask(block: suspend () -> Unit): Job {
        return backgroundScope.launch {
            block()
        }
    }
}