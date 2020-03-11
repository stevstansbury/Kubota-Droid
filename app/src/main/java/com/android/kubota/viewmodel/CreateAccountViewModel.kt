package com.android.kubota.viewmodel

import androidx.lifecycle.*
import com.android.kubota.utility.Utils
import com.kubota.repository.service.CreateAccountService
import com.kubota.repository.service.Response

class CreateAccountViewModel: ViewModel()  {
    private val service = CreateAccountService()

    private val resultsLiveData = MutableLiveData<Response>()
    private val loadingLiveData = MutableLiveData<Boolean>().apply {
        value = false
    }

    val isLoading: LiveData<Boolean> = loadingLiveData
    val result: LiveData<Response> = resultsLiveData

    fun createAccount(email: String, password: String) {
        Utils.backgroundTask {
            val response = service.createAccount(email, password)
            resultsLiveData.postValue(response)
            loadingLiveData.postValue(false)
        }
    }
}

class CreateAccountViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return CreateAccountViewModel() as T
    }
}