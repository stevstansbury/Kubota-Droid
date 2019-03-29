package com.kubota.repository.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import android.text.TextUtils
import java.util.*

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
    val model: String,
    val serialNumber: String?,
    val category: String,
    val manualName: String,
    val manualLocation: String?,
    val hasGuide: Boolean) {


    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        if (other is Model) {
            return userId == other.userId && TextUtils.equals(id, other.id) && TextUtils.equals(model, other.manualName) &&
                    TextUtils.equals(model, other.model) && TextUtils.equals(category, other.category) &&
                    TextUtils.equals(serialNumber, other.serialNumber) && TextUtils.equals(manualLocation, other.manualLocation) &&
                    hasGuide == other.hasGuide
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(id, userId, manualName, model, serialNumber, category, manualLocation, hasGuide)
    }
}