package com.android.kubota.viewmodel.ftue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kubota.repository.service.ChangePasswordService
import com.kubota.repository.service.RequestType
import com.kubota.repository.service.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewPasswordViewModel(private val accessToken: String): ViewModel() {
    private val service = ChangePasswordService()

    suspend fun changePassword(requestType: RequestType): Result {
        return withContext(Dispatchers.IO) {
            service.changePassword(requestType)
        }
    }
}

class NewPasswordViewModelFactory(private val accessToken: String): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return NewPasswordViewModel(accessToken) as T
    }
}