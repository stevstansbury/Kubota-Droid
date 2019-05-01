package com.kubota.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPreference(val userId: String, val models: List<Model>?, val dealers: List<Dealer>?)

@JsonClass(generateAdapter = true)
data class Model(val id: String, val manualName: String, val model: String, val serialNumber: String?, val category: String?)