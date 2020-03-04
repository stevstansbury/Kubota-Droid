package com.android.kubota.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.*
import com.android.kubota.MyKubotaApplication
import com.android.kubota.utility.AccountPrefs
import com.android.kubota.utility.Utils
import com.kubota.repository.service.AuthCredentials
import com.kubota.repository.service.AuthResponse
import com.kubota.repository.service.PreferenceSyncService
import com.kubota.repository.service.SignInService
import com.kubota.repository.user.UserRepo

class SignInViewModel(application: Application, userRepo: UserRepo): AndroidViewModel(application) {

    private val signInService = SignInService(userRepo)

    private val loadingLiveData = MutableLiveData<Boolean>().apply {
        value = false
    }
    private val resultsLiveData = MutableLiveData<AuthResponse>()

    val isLoading: LiveData<Boolean> = loadingLiveData
    val signInResults: LiveData<AuthResponse> = resultsLiveData

    fun signIn(credentials: AuthCredentials) {
        AccountPrefs.clearDisclaimerAccepted(getContext())
        getContext().stopService(Intent(getContext(), PreferenceSyncService::class.java))

        Utils.backgroundTask {
            loadingLiveData.postValue(true)

            val response = signInService.signIn(creds = credentials)
            resultsLiveData.postValue(response)

            loadingLiveData.postValue(false)
        }
    }

    private fun getContext() = getApplication<MyKubotaApplication>().applicationContext
}

class SignInViewModelFactory(
    private val application: Application,
    private val userRepo: UserRepo
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return SignInViewModel(application, userRepo) as T
    }
}