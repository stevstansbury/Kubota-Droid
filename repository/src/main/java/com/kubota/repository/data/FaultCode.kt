package com.kubota.repository.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "fault_code",
    primaryKeys = ["equipmentId", "code"],
    foreignKeys = [ForeignKey(entity = Equipment::class,
        parentColumns = arrayOf("_id"),
        childColumns = arrayOf("equipmentId"),
        onDelete = ForeignKey.CASCADE)])
data class FaultCode(
    val equipmentId: Int = Constants.DEFAULT_ID,
    val code: Int,
    val description: String,
    val action: String)