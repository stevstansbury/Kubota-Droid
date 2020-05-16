package com.android.kubota.viewmodel.ftue

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure

class CreateAccountViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(viewModelClass: Class<T>): T {
        return CreateAccountViewModel() as T
    }
}

class CreateAccountViewModel: ViewModel() {

    companion object {
        fun instance(owner: ViewModelStoreOwner): CreateAccountViewModel {
            return ViewModelProvider(owner, CreateAccountViewModelFactory())
                        .get(CreateAccountViewModel::class.java)
        }
    }

    private val mIsLoading = MutableLiveData(false)
    private val mAccountCreated = MutableLiveData(false)
    private val mError: MutableLiveData<Throwable?> = MutableLiveData(null)

    val isLoading: LiveData<Boolean> = mIsLoading
    val accountCreated: LiveData<Boolean> = mAccountCreated
    val error: LiveData<Throwable?> = mError

    fun createAccount(email: String, password: String) {
        mIsLoading.value = true
        mAccountCreated.value = false
        AppProxy.proxy.accountManager.createAccount(email = email, password = password)
                .done { mAccountCreated.value = true }
                .ensure { mIsLoading.value = false }
                .catch { mError.value = it }
    }

}
