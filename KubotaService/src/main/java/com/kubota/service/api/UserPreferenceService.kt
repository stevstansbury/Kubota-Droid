//
//  UserPreferenceService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.*
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.AppSettings
import com.kubota.service.domain.preference.UserSettings
import java.util.*

enum class UpdateInboxType {
    MarkAsRead,
    MarkAsUnread
}

interface UserPreferenceService {

    fun getEquipment(): Promise<List<EquipmentUnit>>

    fun getEquipment(id: UUID): Promise<EquipmentUnit>

    fun getEquipmentUnit(id: UUID): Promise<EquipmentUnit?>

    fun addEquipmentUnit(request: AddEquipmentUnitRequest): Promise<List<EquipmentUnit>>

    fun removeEquipmentUnit(id: UUID): Promise<List<EquipmentUnit>>

    fun removeEquipmentUnits(units: List<EquipmentUnit>): Promise<List<EquipmentUnit>>

    fun updateEquipmentUnit(update: EquipmentUnitUpdate): Promise<List<EquipmentUnit>>

    fun getDealers(): Promise<List<Dealer>>

    fun getUser(): Promise<User>

    fun addDealer(dealerNumber: String): Promise<List<Dealer>>

    fun removeDealer(dealerNumber: String): Promise<List<Dealer>>

    fun updateGeofence(geofence: Geofence): Promise<List<Geofence>>

    fun createGeofence(description: String, points: List<GeoCoordinate>): Promise<List<Geofence>>

    fun removeGeofence(id: Int): Promise<List<Geofence>>

    fun getGeofences(): Promise<List<Geofence>>

    fun updateEquipmentUnitRestartInhibitStatus(
        id: UUID,
        status: RestartInhibitStatusCode
    ): Promise<EquipmentUnit>

    fun getUserSettings(): Promise<UserSettings>

    fun updateUserSettings(settings: UserSettings): Promise<UserSettings>

    fun getInbox(mostRecentMessageId: UUID? = null, size: Int? = null): Promise<List<InboxMessage>>

    fun updateInboxMessages(type: UpdateInboxType, messages: List<UUID>): Promise<Unit>

    fun deleteInboxMessages(messages: List<UUID>): Promise<Unit>

    fun registerFCMToken(token: String, deviceId: String): Promise<Unit>

    fun deregisterFCMToken(deviceId: String): Promise<Unit>

    fun requestVerifyEmail(): Promise<Unit>

    fun getAppSettings(): Promise<AppSettings>
}
