package com.android.kubota.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Equipment
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.uimodel.EquipmentCategory
import java.util.*

class AddEquipmentViewModel(private val equipmentRepo: EquipmentPreferencesRepo) : ViewModel() {

    fun add(nickName: String, model: String, serialNumber: String, category: String) {
        // TODO: This should be provided server side
        Utils.backgroundTask {
            equipmentRepo.insertEquipment(
                Equipment(
                    serverId = UUID.randomUUID().toString(),
                    userId = 1,
                    model = model,
                    serialNumber = serialNumber,
                    category = category,
                    manualName = "",
                    manualLocation = null,
                    hasGuide = false,
                    nickname = nickName,
                    engineHours = 0,
                    coolantTemperature = null,
                    battery = null,
                    fuelLevel = null,
                    defLevel = null,
                    engineState = null,
                    latitude = null,
                    longitude = null,
                    isVerified = false
                )
            )
        }
    }
}

data class EquipmentUIModel(
    val id: Int,
    val name: String,
    val equipmentCategory: EquipmentCategory,
    val imageResId: Int
) : Parcelable {

    constructor(parcel: Parcel) : this(id = parcel.readInt(), name = parcel.readString() as String,
        equipmentCategory = CategoryUtils.CATEGORY_MAP.getValue(parcel.readString() as String),
        imageResId = parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(equipmentCategory.toString())
        parcel.writeInt(imageResId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EquipmentUIModel> {
        override fun createFromParcel(parcel: Parcel): EquipmentUIModel {
            return EquipmentUIModel(parcel)
        }

        override fun newArray(size: Int): Array<EquipmentUIModel?> {
            return arrayOfNulls(size)
        }
    }
}
