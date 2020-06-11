//
//  Telematics.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.util.*

data class Telematics(
    val locationTime: Date?,
    val cumulativeOperatingHours: Double?,
    val location: GeoCoordinate?,
    val engineRunning: Boolean?,
    val fuelTempCelsius: Int?,
    val fuelRemainingPercent: Int?,
    val defTempCelsius: Int?,
    val defQualityPercent: Double?,
    val defRemainingPercent: Int?,
    val defPressureKPascal: Int?,
    val engineRPM: Int?,
    val coolantTempCelsius: Int?,
    val hydraulicTempCelsius: Int?,
    val extPowerVolts: Double?,
    val airInletTempCelsius: Int?,
    val ambientAirTempCelsius: Double?,
    val runNumber: Int,
    val faultCodes: List<FaultCode>
)
