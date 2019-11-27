package com.kubota.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPreference(val userId: String, @Json(name = "models") val equipments: List<Equipment>?, val dealers: List<Dealer>?)

@JsonClass(generateAdapter = true)
data class Equipment(val id: String, val manualName: String, val model: String, val serialNumber: String?, val category: String?)