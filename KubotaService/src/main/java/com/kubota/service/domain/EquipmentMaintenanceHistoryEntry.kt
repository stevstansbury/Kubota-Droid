package com.kubota.service.domain

import java.util.*

data class EquipmentMaintenanceHistoryEntry(
    val id: String,
    val intervalType: String?,
    val intervalValue: Int?,
    val completedEngineHours: Long?,
    val notes: String?,
    val updatedDate: Date?,
    val maintenanceCheckList: Map<String, Boolean>
)
