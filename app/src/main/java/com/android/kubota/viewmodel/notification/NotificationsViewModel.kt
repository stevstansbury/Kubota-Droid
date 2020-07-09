package com.android.kubota.viewmodel.notification

import androidx.lifecycle.*
import com.android.kubota.app.AppProxy
import com.android.kubota.utility.AuthDelegate
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.kubota.service.api.UpdateInboxType
import com.kubota.service.domain.InboxMessage
import com.kubota.service.domain.InboxMessageSource

class NotificationsViewModelFactory: ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NotificationsViewModel() as T
    }
}

class NotificationsViewModel: ViewModel() {
    private val inboxMessages = MutableLiveData<List<InboxMessage>>()

    val alerts: LiveData<List<InboxMessage>> = Transformations.map(inboxMessages) {
        it.filter { it.sourceFrom == InboxMessageSource.ALERTS }
    }

    val messages: LiveData<List<InboxMessage>> = Transformations.map(inboxMessages) {
        it.filter { it.sourceFrom == InboxMessageSource.MESSAGES }
    }

    fun updateData(delegate: AuthDelegate?) {
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService.getInbox()
            }
            .done {
                inboxMessages.postValue(it)
            }
            .catch {
                val err = it
                val message = it.localizedMessage
            }
    }

    fun markAsRead(delegate: AuthDelegate?, message: InboxMessage) {
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                        .updateInboxMessages(type = UpdateInboxType.MarkAsRead, messages = listOf(message.id))
            }
            .done {
                this.updateData(delegate)
            }
    }

    fun markAsUnread(delegate: AuthDelegate?, message: InboxMessage) {
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .updateInboxMessages(type = UpdateInboxType.MarkAsUnread, messages = listOf(message.id))
            }
            .done {
                this.updateData(delegate)
            }
    }

    fun delete(delegate: AuthDelegate?, message: InboxMessage) {
        AuthPromise(delegate)
            .then {
                AppProxy.proxy.serviceManager.userPreferenceService
                    .deleteInboxMessages(listOf(message.id))
            }
            .done {
                this.updateData(delegate)
            }
    }

}
