package com.android.kubota.viewmodel.ftue

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.Promise

class SignInViewModel: ViewModel() {

    fun signIn(username: String, password: String): Promise<Unit> {
        return AppProxy.proxy.accountManager.authenticate(username = username, password = password)
    }

}

class SignInViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return SignInViewModel() as T
    }

}
