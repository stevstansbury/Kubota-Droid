package com.android.kubota.viewmodel.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done


class UnreadNotificationsViewModelFactory: ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return UnreadNotificationsViewModel() as T
    }
}

open class UnreadNotificationsViewModel: ViewModel() {
    private val mUnreadNotifications = MutableLiveData(0)
    val unreadNotifications: LiveData<Int>
        get() = mUnreadNotifications

    fun loadUnreadNotification(delegate: AuthDelegate?) {
        if (AppProxy.proxy.accountManager.isAuthenticated.value == false) return

        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.getInbox()
            }
            .done {
                mUnreadNotifications.postValue(it.filter { !it.isRead }.size)
            }
    }
}