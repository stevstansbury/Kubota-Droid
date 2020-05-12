package com.kubota.repository.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_suggestion")
data class ModelSuggestion(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = Constants.DEFAULT_ID,
    var searchDate: Long,
    val name: String,
    val category: String
)