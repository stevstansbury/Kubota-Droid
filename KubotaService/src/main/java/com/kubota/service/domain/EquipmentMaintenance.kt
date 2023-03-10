package com.kubota.service.domain

data class EquipmentMaintenance(
    val id: String,
    val checkPoint: String?,
    val measures: String?,
    val firstCheckType: String?,
    val firstCheckValue: Int?,
    val intervalType: String?,
    val intervalValue: Int?,
    val sortOrder: Int
)