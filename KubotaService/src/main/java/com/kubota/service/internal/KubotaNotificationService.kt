package com.kubota.service.internal

import com.couchbase.lite.Database
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.api.NotificationService
import com.kubota.service.domain.Notification
import com.kubota.service.domain.auth.OAuthToken
import java.util.*

internal class KubotaNotificationService(
    config: HTTPService.Config,
    private val couchbaseDb: Database?,
    private val token: OAuthToken?
): HTTPService(config = config), NotificationService {

    override fun getNotifications(): Promise<List<Notification>> {
        TODO("Not yet implemented")
//        val p: Promise<List<Notification>> = service {
//            this.get(
//                route = "/api/user/inbox",
//                type = Types.newParameterizedType(List::class.java, Notification::class.java)
//            )
//        }
//        return p
    }

    override fun markNotificationAsRead(id: UUID): Promise<List<Notification>> {
        TODO("Not yet implemented")
    }

    override fun deleteNotification(id: UUID): Promise<List<Notification>> {
        TODO("Not yet implemented")
//        val p = service {
//            this.delete(
//                route = "/api/user/inbox",
//                body = UploadBody.Json(arrayOf(id))
//            ).asVoid()
//        }
//        return p
    }
}