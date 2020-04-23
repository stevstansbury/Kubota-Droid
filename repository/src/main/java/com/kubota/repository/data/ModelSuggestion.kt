package com.kubota.repository.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val DEFAULT_ID = 0

@Entity(tableName = "model_suggestion")
data class ModelSuggestion(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = DEFAULT_ID,
    val searchDate: Long,
    val name: String,
    val category: String
)