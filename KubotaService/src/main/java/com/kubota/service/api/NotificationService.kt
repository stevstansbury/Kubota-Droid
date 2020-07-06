package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.Notification
import java.util.*

interface NotificationService {
    fun getNotifications(): Promise<List<Notification>>
    fun markNotificationAsRead(id: UUID): Promise<List<Notification>>
    fun deleteNotification(id: UUID): Promise<List<Notification>>
}