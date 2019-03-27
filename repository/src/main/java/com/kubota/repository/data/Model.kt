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
    val manualName: String,
    val model: String,
    val serialNumber: String?,
    val category: String) {


    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        if (other is Model) {
            val temp = other as Model
            return userId == temp.userId && TextUtils.equals(id, temp.id) && TextUtils.equals(model, temp.manualName) &&
                    TextUtils.equals(model, temp.model) && TextUtils.equals(category, temp.category) && TextUtils.equals(serialNumber, temp.serialNumber)
        }

        return false
    }
    override fun hashCode(): Int {
        return Objects.hash(id, userId, manualName, model, serialNumber, category)
    }
}