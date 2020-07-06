package com.kubota.service.domain

import java.util.*

data class EquipmentUnitUpdate(
    val id: UUID,
    val engineHours: Double?,
    val nickName: String?
)
