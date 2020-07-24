//
//  Telematics.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize
import java.util.*

enum class EquipmentMotionState {
    @Json(name="stationary")
    STATIONARY,
    @Json(name="moving")
    MOVING,
    @Json(name="in-transport")
    IN_TRANSPORT,
    @Json(name="unknown")
    UNKNOWN
}

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
    val motionState: EquipmentMotionState?,
    val faultCodes: List<FaultCode>,
    val restartInhibitStatus: RestartInhibitStatus?,
    val insideGeofences: List<Int>?
): Parcelable

val Telematics.outsideGeofence: Boolean
    get() {
        val insideGeofences = this.insideGeofences ?: return true
        return insideGeofences.isEmpty()
    }
