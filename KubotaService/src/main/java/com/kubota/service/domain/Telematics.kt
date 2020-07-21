//
//  Telematics.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
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
    val faultCodes: List<FaultCode>,
    val restartInhibitStatus: RestartInhibitStatus?
): Parcelable

// TODO
val Telematics.outsideGeofence: Boolean
    get() = (this.defRemainingPercent ?: 0) > 50
