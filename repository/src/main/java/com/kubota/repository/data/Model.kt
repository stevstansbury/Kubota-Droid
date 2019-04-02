package com.kubota.repository.data

import android.arch.persistence.room.*
import android.text.TextUtils
import java.util.*

private const val DEFAULT_ID = 0

@Entity(tableName = "models",
        foreignKeys = arrayOf(
            ForeignKey(entity = Account::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("userId"),
                onDelete = ForeignKey.CASCADE)
            ),
        indices = arrayOf(Index(value = arrayOf("serverId"), unique = true)))
data class Model (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = DEFAULT_ID,
    @ColumnInfo(name = "serverId")
    val serverId: String,
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
            return userId == other.userId && id == other.id && TextUtils.equals(serverId, other.serverId) &&
                    TextUtils.equals(manualName, other.manualName) && TextUtils.equals(model, other.model) &&
                    TextUtils.equals(category, other.category) && TextUtils.equals(serialNumber, other.serialNumber) &&
                    TextUtils.equals(manualLocation, other.manualLocation) && hasGuide == other.hasGuide
        }

        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(id, serverId, userId, manualName, model, serialNumber, category, manualLocation, hasGuide)
    }
}