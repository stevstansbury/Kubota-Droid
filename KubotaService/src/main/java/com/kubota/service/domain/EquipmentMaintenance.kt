package com.kubota.service.domain

data class EquipmentMaintenance(
    val checkPoint: String?,
    val measures: String?,
    val firstCheckType: String?,
    val firstCheckValue: Int?,
    val intervalType: String?,
    val intervalValue: Int?
)