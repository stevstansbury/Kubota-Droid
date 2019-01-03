package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "models")
data class Model (
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    val manualName: String,
    val model: String,
    val serialNumber: String?)