package com.android.kubota.viewmodel.ftue

import androidx.lifecycle.*
import com.kubota.repository.service.ChangePasswordService
import com.kubota.repository.service.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForgotPasswordViewModel: ViewModel() {
    private val service = ChangePasswordService()

    suspend fun sendCode(email: String): Result {
        return withContext(Dispatchers.IO) {
            service.requestResetCode(email)
        }
    }
}

class ForgotPasswordViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return ForgotPasswordViewModel() as T
    }
}