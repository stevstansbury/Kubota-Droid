package com.kubota.repository.uimodel

data class Equipment(
    val id: Int,
    val nickname: String?,
    val category: String,
    val model: String,
    val serialNumber: String?,
    val hasManual: Boolean,
    val hasMaintenanceGuides: Boolean,
    val engineHours: Int,
    val telematics: Telematics?
)

data class Telematics(
    val engineStatus: String?,
    val batteryVoltage: Double?,
    val fuelLevel: Double?,
    val defLevel: Double?,
    val location: GeoLocation?,
    val hasFaultCodes: Boolean
)

data class GeoLocation(
    val lat: Double,
    val lng: Double
)