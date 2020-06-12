//
//  UserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.Geofence
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserPreference
import com.kubota.service.domain.preference.UserSettings
import java.util.*

sealed class EquipmentUnitUpdateType(val uuid: UUID) {
    class Nickname(uuid: UUID, val name: String): EquipmentUnitUpdateType(uuid = uuid)
    class UnverifiedEngineHours(uuid: UUID, val hours: Double): EquipmentUnitUpdateType(uuid = uuid)
}

interface UserPreferenceService {

    fun getUserPreference(): Promise<UserPreference>

    fun getUserSettings(): Promise<UserSettings>

    fun updateUserSettings(settings: UserSettings): Promise<UserSettings>

    fun getEquipmentUnit(id: UUID): Promise<EquipmentUnit?>

    fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<UserPreference>

    fun removeEquipmentUnit(id: UUID): Promise<UserPreference>

    fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<UserPreference>

    fun addDealer(id: UUID): Promise<UserPreference>

    fun removeDealer(id: UUID): Promise<UserPreference>

    fun updateEquipmentUnit(type: EquipmentUnitUpdateType): Promise<UserPreference>

    fun updateGeofence(geofence: Geofence): Promise<Unit>

    fun removeGeofence(id: UUID): Promise<Unit>

    fun getGeofences(): Promise<List<Geofence>>
}
