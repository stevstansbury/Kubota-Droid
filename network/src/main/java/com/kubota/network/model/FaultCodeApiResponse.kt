package com.kubota.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FaultCodeApiResponse(val faultCodes: List<FaultCodeModel>)

@JsonClass(generateAdapter = true)
data class FaultCodeModel (
    val code: Int,
    val description: String,
    val accelerationLimited: String?,
    val engineOutputLimited: String?,
    val engineStopped: String?,
    val machinePerformance: String?,
    val provisionalMeasure: String?,
    val dealerTitle: String,
    val customerTitle: String,
    val dealerMessage: String,
    val customerMessage: String
)
