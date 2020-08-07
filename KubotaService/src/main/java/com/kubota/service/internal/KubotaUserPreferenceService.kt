//
//  KubotaUserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import android.os.Parcelable
import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.security.CryptoService
import com.inmotionsoftware.foundation.security.CryptoServiceException
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.UpdateInboxType
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.*
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserSettings
import com.kubota.service.domain.preference.UserSettingsWrapper
import com.kubota.service.internal.couchbase.DictionaryDecoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import com.kubota.service.manager.SettingsRepoFactory
import kotlinx.android.parcel.Parcelize
import java.util.*

private data class UserSettingsDocument(
    val userIdSHA: String,
    val userSettings: UserSettings
)

private data class UserGeofencesDocument(
    val userIdSHA: String,
    val userGeofences: List<Geofence>
)

private data class UserFavoriteDealersDocument(
    val userIdSHA: String,
    val userFavoritDealers: List<Dealer>
)

private data class UserEquipmentDocument(
    val userIdSHA: String,
    val userEquipment: List<EquipmentUnit>
)

@Parcelize
private data class GeofenceUpload(
    val description: String,
    val points: List<GeoCoordinate>
): Parcelable

internal class KubotaUserPreferenceService(
    config: Config,
    private val couchbaseDb: Database?,
    private val token: OAuthToken?
): HTTPService(config = config), UserPreferenceService {

    override fun getEquipment(): Promise<List<EquipmentUnit>> {
         return service {
            val p: Promise<List<EquipmentUnit>> = this.get(route = "/api/user/equipment",
                    type = CodableTypes.newParameterizedType(List::class.java, EquipmentUnit::class.java))
             p
        }
        .then(on = DispatchExecutor.global) { equipment ->
            this.couchbaseDb?.saveUserEquipment(equipment, token = this.token)
            Promise.value(equipment)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val equipment = this.couchbaseDb?.getUserEquipment(this.token) ?: throw error
                    Promise.value(equipment)
                }
            }
        }
    }

    override fun getEquipmentUnit(id: UUID): Promise<EquipmentUnit?> {
        val p: Promise<List<EquipmentUnit>> =
            this.couchbaseDb?.getUserEquipment(this.token)?.let { Promise.value(it) } ?: this.getEquipment()
        return p.map { equipment ->
            equipment.find { it.id == id }
        }
    }

    override fun addEquipmentUnit(request: AddEquipmentUnitRequest, isFromScan: Boolean): Promise<List<EquipmentUnit>> {
        val route = if (isFromScan) "/api/user/equipment/addFromScan" else "/api/user/equipment"
        return service {
            val p: Promise<List<EquipmentUnit>> = this.post(route = route,
                      body = UploadBody.Json(request),
                      type = CodableTypes.newParameterizedType(List::class.java, EquipmentUnit::class.java))
            p
         }
         .map(on = DispatchExecutor.global) { equipment ->
            this.couchbaseDb?.saveUserEquipment(equipment, token = this.token)
            equipment
        }
    }

    override fun removeEquipmentUnit(id: UUID): Promise<List<EquipmentUnit>> {
         return service {
             val p: Promise<List<EquipmentUnit>> = this.delete(route = "/api/user/equipment/${id}",
                        type = CodableTypes.newParameterizedType(List::class.java, EquipmentUnit::class.java))
             p
         }
         .map(on = DispatchExecutor.global) { equipment ->
             this.couchbaseDb?.saveUserEquipment(equipment, token = this.token)
             equipment
         }
    }

    override fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<List<EquipmentUnit>> {
        val tasks = units.map { this.removeEquipmentUnit(id = it.id) }
        return whenFulfilled(tasks).thenMap {
            this.getEquipment()
        }
    }

    override fun updateEquipmentUnit(update: EquipmentUnitUpdate): Promise<List<EquipmentUnit>> {
        return service {
            val p: Promise<List<EquipmentUnit>> = this.put(
                route = "/api/user/equipment/update",
                body = UploadBody.Json(update),
                type = CodableTypes.newParameterizedType(List::class.java, EquipmentUnit::class.java)
            )
            p
        }
        .map(on = DispatchExecutor.global) { equipment ->
            this.couchbaseDb?.saveUserEquipment(equipment, token = this.token)
            equipment
        }
    }

    override fun getDealers(): Promise<List<Dealer>> {
        return service {
            val p: Promise<List<Dealer>> = this.get(route = "/api/user/dealer",
                type = CodableTypes.newParameterizedType(List::class.java, Dealer::class.java))
            p
        }
        .then(on = DispatchExecutor.global) { dealers ->
            this.couchbaseDb?.saveUserDealers(dealers, token = this.token)
            Promise.value(dealers)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val dealers = this.couchbaseDb?.getUserDealers(this.token) ?: throw error
                    Promise.value(dealers)
                }
            }
        }
    }

    override fun addDealer(dealerNumber: Int): Promise<List<Dealer>> {
        return service {
            val p: Promise<List<Dealer>> =
                this.post(route = "/api/user/dealer/${dealerNumber}",
                      body = UploadBody.Empty(),
                      type = CodableTypes.newParameterizedType(List::class.java, Dealer::class.java))
            p
        }
        .map(on = DispatchExecutor.global) { dealers ->
            this.couchbaseDb?.saveUserDealers(dealers, token = this.token)
            dealers
        }
    }

    override fun removeDealer(dealerNumber: Int): Promise<List<Dealer>> {
        return service {
            val p: Promise<List<Dealer>> =
                this.delete(route = "/api/user/dealer/${dealerNumber}",
                        type = CodableTypes.newParameterizedType(List::class.java, Dealer::class.java))
            p
        }
        .map(on = DispatchExecutor.global) { dealers ->
            this.couchbaseDb?.saveUserDealers(dealers, token = this.token)
            dealers
        }
    }

    override fun removeGeofence(id: Int): Promise<List<Geofence>> {
        val p: Promise<List<Geofence>> =
            this.delete(route = "/api/user/geofence/${id}",
                        type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java))

        return service { p }.map(on = DispatchExecutor.global) { geofences ->
            this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
            geofences
        }
    }

    override fun createGeofence(description: String, points: List<GeoCoordinate>): Promise<List<Geofence>> {
        val upload = GeofenceUpload(description, points)
        val p: Promise<List<Geofence>> =
            this.post(
                route = "/api/user/geofence",
                body = UploadBody.Json(upload),
                type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java)
            )

        return service { p }.map(on = DispatchExecutor.global) { geofences ->
            this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
            geofences
        }
    }

    override fun updateGeofence(geofence: Geofence): Promise<List<Geofence>> {
        val p: Promise<List<Geofence>> =
            this.put(
                route = "/api/user/geofence",
                body = UploadBody.Json(geofence),
                type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java)
            )

        return service { p }.map(on = DispatchExecutor.global) { geofences ->
            this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
            geofences
        }
    }

    override fun registerFCMToken(token: String): Promise<Unit> {
        val query = QueryParameters()
        query.addQueryParameter("token", token)
        query.addQueryParameter("platform", "android")
        return service {
            this.post(
                route="/notification/fcm-token",
                query=query,
                body=UploadBody.Empty()
            ).asVoid()
        }
    }

    override fun getGeofences(): Promise<List<Geofence>> {
        val p: Promise<List<Geofence>> = this.get(route = "/api/user/geofence",
                                                  type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java))
        return service { p }.then(on = DispatchExecutor.global) { geofences ->
                    this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
                    Promise.value(geofences)
                }
                .recover(on = DispatchExecutor.global) {err ->
                    val error = err as? KubotaServiceError ?: throw err
                    when (error) {
                        is KubotaServiceError.Unauthorized -> throw error
                        else -> {
                            val prefs = this.couchbaseDb?.getUserGeofences(this.token) ?: throw error
                            Promise.value(prefs)
                        }
                    }
                }
    }

    override fun updateEquipmentUnitRestartInhibitStatus(
        id: UUID,
        status: RestartInhibitStatusCode
    ): Promise<EquipmentUnit> {
        return service {
            val p: Promise<List<EquipmentUnit>> = this.put(route = "/api/user/equipment/${id}/restartInhibit"
                    , query = queryParams("status" to status.toString())
                    , body = UploadBody.Empty()
                    , type = CodableTypes.newParameterizedType(List::class.java, EquipmentUnit::class.java)
                )
            p.map(on = DispatchExecutor.global) { equipment ->
                this.couchbaseDb?.saveUserEquipment(equipment, token = this.token)
                equipment.first { it.id == id }
            }
        }
    }

    override fun getUserSettings(): Promise<UserSettings> {
        val p: Promise<UserSettingsWrapper> = service {
            this.get(route = "/api/user/settings", type = UserSettingsWrapper::class.java)
        }
        return p.thenMap(on = DispatchExecutor.global) { resp ->
            this.couchbaseDb?.saveUserSettings(resp.settings, token = this.token)
            SettingsRepoFactory.getUserSettingsRepo().saveUserSettings(resp.settings)
            Promise.value(resp.settings)
        }
            .recover(on = DispatchExecutor.global) { err ->
                val error = err as? KubotaServiceError ?: throw err
                when (error) {
                    is KubotaServiceError.Unauthorized -> {
                        SettingsRepoFactory.getUserSettingsRepo().saveUserSettings()
                        throw error
                    }
                    else -> {
                        val settings = this.couchbaseDb?.getUserSettings(this.token) ?: throw error
                        Promise.value(settings)
                    }
                }
            }
    }

    override fun updateUserSettings(settings: UserSettings): Promise<UserSettings> {
        val p: Promise<UserSettingsWrapper> = service {
            this.post(route = "/api/user/settings",
                body = UploadBody.Json(UserSettingsWrapper(settings)),
                type = UserSettingsWrapper::class.java)
        }
        return p.map(on = DispatchExecutor.global) { resp ->
            this.couchbaseDb?.saveUserSettings(resp.settings, token = this.token)
            SettingsRepoFactory.getUserSettingsRepo().saveUserSettings(resp.settings)
            resp.settings
        }
    }

    override fun getInbox(mostRecentMessageId: UUID?, size: Int?): Promise<List<InboxMessage>> {
        val query = QueryParameters()
        mostRecentMessageId?.let {
            query.addQueryParameter("mostRecentMessageId", it.toString())
        }
        size?.let {
            query.addQueryParameter("size", it.toString())
        }

        val p: Promise<List<InboxMessage>> =
            this.get(route = "/api/user/inbox",
                    query = if (query.fields.isNotEmpty()) query else null,
                    type = CodableTypes.newParameterizedType(List::class.java, InboxMessage::class.java))
        return service { p }
    }

    override fun updateInboxMessages(type: UpdateInboxType, messages: List<UUID>): Promise<Unit> {
        val route = when (type) {
            UpdateInboxType.MarkAsRead -> {
                "/api/user/inbox/markAsRead"
            }
            UpdateInboxType.MarkAsUnread -> {
                "/api/user/inbox/markAsUnread"
            }
        }
        return service {
            this.put(
                route = route,
                query = queryParamsMultiValue(
                    "messageId" to messages.map { it.toString() }
                ),
                body = UploadBody.Empty()
            ).asVoid()
        }
    }

    override fun deleteInboxMessages(messages: List<UUID>): Promise<Unit> {
        val params = queryParamsMultiValue(
            "messageId" to messages.map { it.toString() }
        )
        return service {
            this.delete(route = "/api/user/inbox", query = params)
        }
    }

}

private fun String.sha256(): String? {
    return try {
        CryptoService.getSHA256(string = this)
    } catch (e: CryptoServiceException) {
        null
    }
}

@Throws
private fun Database.saveUserEquipment(equipment: List<EquipmentUnit>, token: OAuthToken?) {
    // Using accessToken to identify user since we don't have other equivalent information
    val userIdSHA = token?.accessToken?.sha256() ?: return
    val userEquipment = UserEquipmentDocument(userIdSHA = userIdSHA, userEquipment = equipment)
    val data = DictionaryEncoder().encode(userEquipment) ?: return
    val document = MutableDocument("UserEquipmentDocument", data)
    this.save(document)
}

@Throws
private fun Database.getUserEquipment(token: OAuthToken?): List<EquipmentUnit>? {
    val userIdSHA = token?.accessToken?.sha256() ?: return null
    val document = this.getDocument("UserEquipmentDocument") ?: return null

    val data = document.toMap()
    val userDoc = DictionaryDecoder().decode(type = UserEquipmentDocument::class.java, value = data)
    if (userDoc?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return userDoc.userEquipment
}

@Throws
private fun Database.saveUserDealers(dealers: List<Dealer>, token: OAuthToken?) {
    // Using accessToken to identify user since we don't have other equivalent information
    val userIdSHA = token?.accessToken?.sha256() ?: return
    val userDealers = UserFavoriteDealersDocument(userIdSHA = userIdSHA, userFavoritDealers = dealers)
    val data = DictionaryEncoder().encode(userDealers) ?: return
    val document = MutableDocument("UserFavoriteDealersDocument", data)
    this.save(document)
}

@Throws
private fun Database.getUserDealers(token: OAuthToken?): List<Dealer>? {
    val userIdSHA = token?.accessToken?.sha256() ?: return null
    val document = this.getDocument("UserFavoriteDealersDocument") ?: return null

    val data = document.toMap()
    val userDoc = DictionaryDecoder().decode(type = UserFavoriteDealersDocument::class.java, value = data)
    if (userDoc?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return userDoc.userFavoritDealers
}

@Throws
private fun Database.saveUserSettings(settings: UserSettings, token: OAuthToken?) {
    // Using accessToken to identify user since we don't have other equivalent information
    val userIdSHA = token?.accessToken?.sha256() ?: return
    val userSettings = UserSettingsDocument(userIdSHA = userIdSHA, userSettings = settings)
    val data = DictionaryEncoder().encode(userSettings) ?: return
    val document = MutableDocument("UserSettingsDocument", data)
    this.save(document)
}

@Throws
private fun Database.getUserSettings(token: OAuthToken?): UserSettings? {
    val userIdSHA = token?.accessToken?.sha256() ?: return null
    val document = this.getDocument("UserSettingsDocument") ?: return null

    val data = document.toMap()
    val settings = DictionaryDecoder().decode(type = UserSettingsDocument::class.java, value = data)
    if (settings?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return settings.userSettings
}

@Throws
private fun Database.saveUserGeofences(geofences: List<Geofence>, token: OAuthToken?) {
    // Using accessToken to identify user since we don't have other equivalent information
    val userIdSHA = token?.accessToken?.sha256() ?: return
    val userGeofences = UserGeofencesDocument(userIdSHA = userIdSHA, userGeofences = geofences)
    val data = DictionaryEncoder().encode(userGeofences) ?: return
    val document = MutableDocument("UserGeofencesDocument", data)
    this.save(document)
}

@Throws
private fun Database.getUserGeofences(token: OAuthToken?): List<Geofence>? {
    val userIdSHA = token?.accessToken?.sha256() ?: return null
    val document = this.getDocument("UserGeofencesDocument") ?: return null

    val data = document.toMap()
    val geofences = DictionaryDecoder().decode(type = UserGeofencesDocument::class.java, value = data)
    if (geofences?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return geofences.userGeofences
}
