package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "selected_models")
data class SelectedModel(
    @ForeignKey(
        entity = Model::class,
        parentColumns = ["id"],
        childColumns = ["modelId"],
        onDelete = ForeignKey.CASCADE)
    val modelId: String) {
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1;
}

@Entity(tableName = "selected_dealers")
data class SelectedDealer(
    @ForeignKey(
        entity = Dealer::class,
        parentColumns = ["id"],
        childColumns = ["dealerId"],
        onDelete = ForeignKey.CASCADE)
    val dealerId: String) {
    val id: Int = 1;
}