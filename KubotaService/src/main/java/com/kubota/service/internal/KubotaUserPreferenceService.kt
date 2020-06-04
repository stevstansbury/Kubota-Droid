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
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserPreference
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import java.util.*

private data class UserPreferenceDocument(
    val userIdSHA: String,
    val userPreference: UserPreference
)

internal data class UnverifiedEngineHoursParams(val id: String, val engineHours: Double)

internal class KubotaUserPreferenceService(
    config: Config,
    private val couchbaseDb: Database?,
    private val token: OAuthToken?
): HTTPService(config = config), UserPreferenceService {

    override fun getUserPreference(): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.get(route = "/api/user/preferences", type = UserPreference::class.java)
        }
        return p.then(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            Promise.value(prefs)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val prefs = this.couchbaseDb?.getUserPreference(this.token) ?: throw error
                    Promise.value(prefs)
                }
            }
        }
    }

    override fun getEquipmentUnit(id: UUID): Promise<EquipmentUnit?> {
        val p: Promise<UserPreference> = this.couchbaseDb?.getUserPreference(this.token)?.let { Promise.value(it) } ?: this.getUserPreference()
        return p.map { pref ->
            pref.equipment?.find { it.id == id }
        }
    }

    override fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.post(route = "/api/user/preferences/equipment", body = UploadBody.Json(request), type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            prefs
        }
    }

    override fun removeEquipmentUnit(id: UUID): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.delete(route = "/api/user/preferences/equipment/${id}", type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            prefs
        }
    }

    override fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<UserPreference> {
        val tasks = units.map { this.removeEquipmentUnit(id = it.id) }
        return whenFulfilled(tasks).thenMap {
            this.getUserPreference()
        }
    }

    override fun addDealer(id: UUID): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.post(route = "/api/user/preferences/dealer/${id}", body = UploadBody.Empty(), type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            prefs
        }
    }

    override fun removeDealer(id: UUID): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.delete(route = "/api/user/preferences/dealer/${id}", type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            prefs
        }
    }

    override fun updateEquipmentUnit(type: EquipmentUnitUpdateType): Promise<UserPreference> {
        val route = "/api/user/preferences/equipment"
        val p: Promise<UserPreference> = service {
            when (type) {
                is EquipmentUnitUpdateType.Nickname ->
                    this.put(
                        route = "${route}/nickName",
                        body = UploadBody.Json(mapOf("id" to type.uuid.toString(),"nickName" to type.name)),
                        type = UserPreference::class.java
                    )
                is EquipmentUnitUpdateType.UnverifiedEngineHours ->
                    this.put(
                        route = "${route}/engineHours",
                        body = UploadBody.Json(UnverifiedEngineHoursParams(id = type.uuid.toString(), engineHours = type.hours)),
                        type = UserPreference::class.java
                    )
            }
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs, token = this.token)
            prefs
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
private fun Database.saveUserPreference(prefs: UserPreference, token: OAuthToken?) {
    // Using accessToken to identify user since we don't have other equivalent information
    val userIdSHA = token?.accessToken?.sha256() ?: return
    val userPref = UserPreferenceDocument(userIdSHA = userIdSHA, userPreference = prefs)
    val data = DictionaryEncoder().encode(userPref) ?: return
    val document = MutableDocument("UserPreferenceDocument", data)
    this.save(document)
}

@Throws
private fun Database.getUserPreference(token: OAuthToken?): UserPreference? {
    val userIdSHA = token?.accessToken?.sha256() ?: return null
    val document = this.getDocument("UserPreferenceDocument") ?: return null

    val data = document.toMap()
    val userPref = DictionaryDeccoder().decode(type = UserPreferenceDocument::class.java, value = data)
    if (userPref?.userIdSHA != userIdSHA) {
        this.delete(document)
        return null
    }
    return userPref.userPreference
}
