//
//  Telematics.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import com.squareup.moshi.Json
import androidx.core.math.MathUtils
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

enum class TelematicStatus {
    Normal,
    Warning,
    Critical,
}

fun fuelStatus(level: Int): TelematicStatus {
    val fuel = MathUtils.clamp(level, 0, 100)
    return when (fuel) {
        in 26..100 -> TelematicStatus.Normal
        in 11..25 -> TelematicStatus.Warning
        in 0..10 -> TelematicStatus.Critical
        else -> TelematicStatus.Critical
    }
}

fun defStatus(level: Int): TelematicStatus {
    val fuel = MathUtils.clamp(level, 0, 100)
    return when (fuel) {
        in 36..100 -> TelematicStatus.Normal
        in 16..35 -> TelematicStatus.Warning
        in 0..15 -> TelematicStatus.Critical
        else -> TelematicStatus.Critical
    }
}

fun voltageStatus(level: Double): TelematicStatus {
    return if (level >= 12.5) {
        TelematicStatus.Normal
    } else if (level >= 12.3) {
        TelematicStatus.Warning
    } else {
        TelematicStatus.Critical
    }
}

fun tempStatus(level: Int): TelematicStatus {
    val temp = Math.max(level, 0)
    return when (temp) {
        in 0..100 -> TelematicStatus.Normal
        in 101..110 -> TelematicStatus.Warning
        else -> TelematicStatus.Critical
    }
}

fun hydraulicTemp(level: Int): TelematicStatus = tempStatus(level)
fun coolantTemp(level: Int): TelematicStatus = tempStatus(level)

val Telematics.fuelRemainingStatus: TelematicStatus? get() = fuelRemainingPercent?.let { fuelStatus(it) }
val Telematics.defRemainingStatus: TelematicStatus? get() = defRemainingPercent?.let { defStatus((it)) }
val Telematics.voltageStatus: TelematicStatus? get() = extPowerVolts?.let { voltageStatus(it) }
val Telematics.hydraulicTempStatus: TelematicStatus? get() = hydraulicTempCelsius?.let { hydraulicTemp(it) }
val Telematics.coolantTempStatus: TelematicStatus? get() = coolantTempCelsius?.let { coolantTemp(it) }

val Telematics.outsideGeofence: Boolean
    get() = this.insideGeofences?.isEmpty() ?: false