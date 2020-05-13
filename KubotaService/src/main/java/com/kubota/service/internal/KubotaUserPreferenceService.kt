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
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserPreference
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import java.util.*

internal data class UnverifiedEngineHoursParams(val id: String, val engineHours: Double)

internal class KubotaUserPreferenceService(config: Config, private val couchbaseDb: Database?): HTTPService(config = config), UserPreferenceService {

    override fun getUserPreference(): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.get(route = "/api/user/preferences", type = UserPreference::class.java)
        }
        return p.then(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs)
            Promise.value(prefs)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val prefs = this.couchbaseDb?.getUserPreference() ?: throw error
                    Promise.value(prefs)
                }
            }
        }
    }

    override fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.post(route = "/api/user/preferences/equipment", body = UploadBody.Json(request), type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs)
            prefs
        }
    }

    override fun removeEquipmentUnit(id: UUID): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.delete(route = "/api/user/preferences/equipment/${id}", type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs)
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
            this.couchbaseDb?.saveUserPreference(prefs)
            prefs
        }
    }

    override fun removeDealer(id: UUID): Promise<UserPreference> {
        val p: Promise<UserPreference> = service {
            this.delete(route = "/api/user/preferences/dealer/${id}", type = UserPreference::class.java)
        }
        return p.map(on = DispatchExecutor.global) { prefs ->
            this.couchbaseDb?.saveUserPreference(prefs)
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
            this.couchbaseDb?.saveUserPreference(prefs)
            prefs
        }
    }

}

@Throws
private fun Database.saveUserPreference(prefs: UserPreference) {
    val data = DictionaryEncoder().encode(prefs) ?: return
    val document = MutableDocument("UserPreference", data)
    this.save(document)
}

@Throws
private fun Database.getUserPreference(): UserPreference? {
    val document = this.getDocument("UserPreference") ?: return null
    val data = document.toMap()
    return DictionaryDeccoder().decode(type = UserPreference::class.java, value = data)
}
