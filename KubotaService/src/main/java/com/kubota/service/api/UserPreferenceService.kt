//
//  UserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.Dealer
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.EquipmentUnitUpdate
import com.kubota.service.domain.Geofence
import com.kubota.service.domain.RestartInhibitStatus
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.UserSettings
import java.util.*


interface UserPreferenceService {

    fun getEquipment(): Promise<List<EquipmentUnit>>

    fun getEquipmentUnit(id: UUID): Promise<EquipmentUnit?>

    fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<List<EquipmentUnit>>

    fun removeEquipmentUnit(id: UUID): Promise<List<EquipmentUnit>>

    fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<List<EquipmentUnit>>

    fun updateEquipmentUnit(update: EquipmentUnitUpdate): Promise<List<EquipmentUnit>>

    fun getDealers(): Promise<List<Dealer>>

    fun addDealer(dealerNumber: Int): Promise<List<Dealer>>

    fun removeDealer(dealerNumber: Int): Promise<List<Dealer>>

    fun updateGeofence(geofence: Geofence): Promise<List<Geofence>>

    fun removeGeofence(id: UUID): Promise<List<Geofence>>

    fun getGeofences(): Promise<List<Geofence>>

    fun updateEquipmentUnitRestartInhibitStatus(id: UUID, status: RestartInhibitStatus): Promise<Unit>

    fun getUserSettings(): Promise<UserSettings>

    fun updateUserSettings(settings: UserSettings): Promise<UserSettings>
}
