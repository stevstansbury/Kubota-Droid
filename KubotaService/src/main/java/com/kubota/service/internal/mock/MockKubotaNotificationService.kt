package com.kubota.service.internal.mock

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.api.NotificationService
import com.kubota.service.domain.Notification
import com.kubota.service.domain.NotificationSource
import java.util.*

class MockKubotaNotificationService: NotificationService {

    val notificationList = mutableListOf<Notification>(
        //NOTIFICATIONS FROM MESSAGES
        Notification(
            id = UUID.randomUUID(),
            title = "Buy 4 tires and get 0 tires for free",
            body = "Take advantage of this month's tire promotion. Talk to your local dealership today!",
            sourceFrom = NotificationSource.MESSAGES,
            isRead = false,
            createdTime = ""
        ),
        Notification(
            id = UUID.randomUUID(),
            title = "$0 Down, 0% interest and 0 Payments for a year",
            body = "Take advantage of this month's financing promotion. Talk to your local dealership today!",
            sourceFrom = NotificationSource.MESSAGES,
            isRead = false,
            createdTime = ""
        ),
        Notification(
            id = UUID.randomUUID(),
            title = "Buy a tractor get a tractor sale",
            body = "You buy a tractor and you get to take it home. Contact your dealership today!",
            sourceFrom = NotificationSource.MESSAGES,
            isRead = true,
            createdTime = ""
        ),
        //NOTIFICATIONS FROM TELEMATICS
        Notification(
            id = UUID.randomUUID(),
            title = "Fatal Error",
            body = "Please contact your dealership.",
            sourceFrom = NotificationSource.ALERTS,
            isRead = false,
            createdTime = ""
        ),
        Notification(
            id = UUID.randomUUID(),
            title = "Geofence Error",
            body = "Your tractor is outside of it's geofence area.",
            sourceFrom = NotificationSource.ALERTS,
            isRead = true,
            createdTime = ""
        ),
        Notification(
            id = UUID.randomUUID(),
            title = "Fault Code Detected",
            body = "Error 9200 has been detected with your tractor.",
            sourceFrom = NotificationSource.ALERTS,
            isRead = true,
            createdTime = ""
        )
    )

    override fun getNotifications(): Promise<List<Notification>> {
        return Promise.value(notificationList)
    }

    override fun deleteNotification(id: UUID): Promise<List<Notification>> {
        notificationList.removeAll { it.id == id }
        return Promise.value(notificationList)
    }

    override fun markNotificationAsRead(id: UUID): Promise<List<Notification>> {
        var idx = 0
        notificationList.forEach {
            if (it.id == id) {
                val newNotification = it.copy(isRead = false)
                notificationList.set(idx, newNotification)
            }
            idx++
        }

        return Promise.value(notificationList)
    }
}