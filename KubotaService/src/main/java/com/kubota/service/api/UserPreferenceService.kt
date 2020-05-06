//
//  UserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserPreference
import java.util.*

sealed class EquipmentUnitUpdateType(val uuid: UUID) {
    class Nickname(uuid: UUID, val name: String): EquipmentUnitUpdateType(uuid = uuid)
    class UnverifiedEngineHours(uuid: UUID, val hours: Double): EquipmentUnitUpdateType(uuid = uuid)
}

interface UserPreferenceService {

    fun getUserPreference(): Promise<UserPreference>

    fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<UserPreference>

    fun removeEquipmentUnit(uuid: UUID): Promise<UserPreference>

    fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<UserPreference>

    fun addDealer(uuid: UUID): Promise<UserPreference>

    fun removeDealer(uuid: UUID): Promise<UserPreference>

    fun updateEquipmentUnit(type: EquipmentUnitUpdateType): Promise<UserPreference>

}
