package com.kubota.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EquipmentModelResult(
    val models: List<EquipmentModel>
)

@JsonClass(generateAdapter = true)
data class EquipmentModel(
    val model: String,
    val category: String,
    val guideUrl: String?
)