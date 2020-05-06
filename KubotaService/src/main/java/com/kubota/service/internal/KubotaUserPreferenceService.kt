//
//  KubotaUserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.asVoid
import com.kubota.service.api.EquipmentUnitUpdateType
import com.kubota.service.api.UserPreferenceService
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserPreference
import java.util.*

internal data class UnverifiedEngineHoursParams(val id: String, val engineHours: Double)

internal class KubotaUserPreferenceService(config: Config): HTTPService(config = config), UserPreferenceService {

    override fun getUserPreference(): Promise<UserPreference> {
        return service {
            this.get(route = "/api/user/preferences", type = UserPreference::class.java)
        }
    }

    override fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<UserPreference> {
        return service {
            this.post(
                route = "/api/user/preferences/equipment",
                body = UploadBody.Json(request),
                type = UserPreference::class.java
            )
        }
    }

    override fun removeEquipmentUnit(uuid: UUID): Promise<UserPreference> {
        return service {
            this.delete(route = "/api/user/preferences/equipment/${uuid}", type = UserPreference::class.java)
        }
    }

    override fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<UserPreference> {
        TODO("Not yet implemented")
    }

    override fun addDealer(uuid: UUID): Promise<UserPreference> {
        return service {
            this.post(route = "/api/user/preferences/dealer/${uuid}", body = UploadBody.Empty(), type = UserPreference::class.java)
        }
    }

    override fun removeDealer(uuid: UUID): Promise<UserPreference> {
        return service {
            this.delete(route = "/api/user/preferences/dealer/${uuid}", type = UserPreference::class.java)
        }
    }

    override fun updateEquipmentUnit(type: EquipmentUnitUpdateType): Promise<UserPreference> {
        val route = "/api/user/preferences/equipment"
        return service {
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
    }

}
