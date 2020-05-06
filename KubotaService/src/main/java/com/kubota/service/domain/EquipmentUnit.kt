//
//  EquipmentUnit.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.util.*

data class EquipmentUnit(
    val id: UUID,
    val manualLocation: String,
    val model: String,
    val category: String?,
    val identifierType: String,
    val pinOrSerial: String?,
    val pin: String?,
    val serial: String?,
    val nickName: String?,
    val engineHours: Double?,
    val engineRunning: Boolean?,
    val location: Location?,
    val batteryVoltage: Double?,
    val fuelLevelPercent: Int?,
    val defLevelPercent: Int?,
    val coolantTemperatureCelsius: Int?,
    val faultCodes: List<Int>,
    val hasTelematics: Boolean
)
