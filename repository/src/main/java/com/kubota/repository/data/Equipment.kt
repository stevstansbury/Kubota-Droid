package com.kubota.repository.data

import androidx.room.*
import android.text.TextUtils
import java.util.*

@Entity(tableName = "equipments",
        foreignKeys = [ForeignKey(entity = Account::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("userId"),
            onDelete = ForeignKey.CASCADE)],
        indices = [Index(value = arrayOf("serverId"), unique = true)]
)
data class Equipment (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Int = Constants.DEFAULT_ID,
    @ColumnInfo(name = "serverId")
    val serverId: String,
    val userId: Int,
    val model: String,
    val serialNumber: String?,
    val category: String,
    val manualName: String,
    val manualLocation: String?,
    val hasGuide: Boolean,
    val nickname: String? = null,
    @ColumnInfo(defaultValue = "0")
    val engineHours: Int = 0,
    val coolantTemperature: Int?,
    val battery: Double?,
    val fuelLevel: Int?,
    val defLevel: Int?,
    val engineState: Boolean?,
    val latitude: Double?,
    val longitude: Double?,
    val isVerified: Boolean) {


    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Equipment) return false

        return userId == other.userId && id == other.id && TextUtils.equals(serverId, other.serverId) &&
                TextUtils.equals(manualName, other.manualName) && TextUtils.equals(model, other.model) &&
                TextUtils.equals(category, other.category) && TextUtils.equals(serialNumber, other.serialNumber) &&
                TextUtils.equals(manualLocation, other.manualLocation) && hasGuide == other.hasGuide &&
                TextUtils.equals(nickname, other.nickname) && engineHours == other.engineHours &&
                coolantTemperature == other.coolantTemperature && battery == other.battery &&
                fuelLevel == other.fuelLevel && defLevel == other.defLevel &&
                engineState == other.engineState && latitude == other.latitude
                && longitude == other.longitude
    }

    override fun hashCode(): Int {
        return Objects.hash(id, serverId, userId, manualName, model, serialNumber, category,
            manualLocation, hasGuide, nickname, engineHours, coolantTemperature, battery, fuelLevel,
            defLevel, engineState, latitude, longitude)
    }
}