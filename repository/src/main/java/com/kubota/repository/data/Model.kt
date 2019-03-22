package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "models",
        foreignKeys = arrayOf(
            ForeignKey(entity = Account::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("userId"),
                onDelete = ForeignKey.CASCADE)
            ))
data class Model (
    @PrimaryKey @ColumnInfo(name = "id")
    val id: String,
    val userId: Int,
    val manualName: String,
    val model: String,
    val serialNumber: String?,
    val category: String)