package com.android.kubota.viewmodel.notification

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.inmotionsoftware.promisekt.done
import com.kubota.service.domain.Notification
import com.kubota.service.domain.NotificationSource

class NotificationsViewModelFactory: ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NotificationsViewModel() as T
    }
}

class NotificationsViewModel: ViewModel() {
    private val notifications = MutableLiveData<List<Notification>>()

    val alerts: LiveData<List<Notification>> = Transformations.map(notifications) {
        it.filter { it.sourceFrom == NotificationSource.ALERTS }
    }

    val messages: LiveData<List<Notification>> = Transformations.map(notifications) {
        it.filter { it.sourceFrom == NotificationSource.MESSAGES }
    }

    init {
        AppProxy.proxy.serviceManager.notificationService.getNotifications()
            .done { notifications.postValue(it) }
    }

    fun markNotificationAsRead(notification: Notification) {
        AppProxy.proxy.serviceManager.notificationService.markNotificationAsRead(notification.id)
            .done { notifications.postValue(it) }
    }

    fun deleteNotification(notification: Notification) {
        AppProxy.proxy.serviceManager.notificationService.deleteNotification(notification.id)
            .done { notifications.postValue(it) }
    }
}