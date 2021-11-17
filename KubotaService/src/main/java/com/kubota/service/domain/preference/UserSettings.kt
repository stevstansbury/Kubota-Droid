//
//  UserSettings.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain.preference

internal data class UserSettingsWrapper(val settings: UserSettings)

enum class MeasurementUnitType {
    US,
    METRIC
}

data class UserSettings(
    val measurementUnit: MeasurementUnitType?,
    val subscribedToNotifications: Boolean?,
    val subscribedToMessages: Boolean?,
    val subscribedToAlerts: Boolean?
)

data class AppSettings(
    val minVersionAndroid: String
)
