//
//  KubotaUserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.security.CryptoService
import com.inmotionsoftware.foundation.security.CryptoServiceException
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.*
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserSettings
import com.kubota.service.domain.preference.UserSettingsWrapper
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import com.kubota.service.manager.SettingsRepoFactory
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

    override fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<List<EquipmentUnit>> {
        return service {
            val p: Promise<List<EquipmentUnit>> = this.post(route = "/api/user/equipment",
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

    override fun removeGeofence(id: UUID): Promise<List<Geofence>> {
        val p: Promise<List<Geofence>> =
            this.delete(route = "/api/user/geofence/${id}",
                        type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java))

        return service { p }.map(on = DispatchExecutor.global) { geofences ->
            this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
            geofences
        }
    }

    override fun updateGeofence(geofence: Geofence): Promise<List<Geofence>> {
        val p: Promise<List<Geofence>> =
            this.post(
                route = "/api/user/geofence",
                body = UploadBody.Json(geofence),
                type = CodableTypes.newParameterizedType(List::class.java, Geofence::class.java)
            )

        return service { p }.map(on = DispatchExecutor.global) { geofences ->
            this.couchbaseDb?.saveUserGeofences(geofences, token = this.token)
            geofences
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
    ): Promise<Unit> {
        return service {
            this.put(route = "/api/user/equipment/${id}/restartInhibit"
                    , query = queryParams("status" to status.toString())
                     , body = UploadBody.Empty()
                )
                .asVoid()
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
    val userDoc = DictionaryDeccoder().decode(type = UserEquipmentDocument::class.java, value = data)
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
    val userDoc = DictionaryDeccoder().decode(type = UserFavoriteDealersDocument::class.java, value = data)
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
    val settings = DictionaryDeccoder().decode(type = UserSettingsDocument::class.java, value = data)
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
    val geofences = DictionaryDeccoder().decode(type = UserGeofencesDocument::class.java, value = data)
    if (geofences?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return geofences.userGeofences
}
