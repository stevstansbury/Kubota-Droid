package com.kubota.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPreference(
    val userId: String,
    @Json(name = "equipment")
    val equipments: List<Equipment>?,
    val dealers: List<Dealer>?
)

@JsonClass(generateAdapter = true)
data class Equipment(
    val id: String,
    @Json(name = "manualLocation")
    val manualName: String,
    val model: String,
    val pinOrSerial: String?,
    val category: String?,
    @Json(name = "nickName")
    val nickname: String?,
    val engineHours: Double? = null,
    val location: Location?,
    val fuelLevelPercent: Int?,
    val defLevelPercent: Int?,
    val faultCodes: List<Int>,
    @Json(name = "batteryVoltage")
    val batteryVolt: Double? = null,
    @Json(name = "engineRunning")
    val isEngineRunning: Boolean? = null,
    val hasTelematics: Boolean
)

data class Location(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "altitudeMeters")
    val altitude: Double
)