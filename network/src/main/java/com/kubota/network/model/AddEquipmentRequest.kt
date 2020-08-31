package com.kubota.network.model

data class AddEquipmentRequest(
    val identifierType: EquipmentIdentifier,
    val pinOrSerial: String?,
    val model: String,
    val nickName: String?,
    val engineHours: Double
)

enum class EquipmentIdentifier {
    Pin,
    Serial,
    None
}