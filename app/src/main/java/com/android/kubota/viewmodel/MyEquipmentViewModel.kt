package com.android.kubota.viewmodel

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.AuthPromise
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.catch
import com.inmotionsoftware.promisekt.done
import com.inmotionsoftware.promisekt.ensure
import com.inmotionsoftware.promisekt.features.whenFulfilled
import com.kubota.service.domain.EquipmentUnit
import com.kubota.service.domain.preference.AddEquipmentUnitRequest
import com.kubota.service.domain.preference.EquipmentUnitIdentifier

data class UIEquipment(
    val id: Int,
    val nickname: String?,
    val model: String,
    val serialNumber: String?,
    @StringRes
    val categoryResId: Int,
    @DrawableRes
    val imageResId: Int,
    val hasManual: Boolean,
    val hasMaintenanceGuides: Boolean,
    val engineHours: Int
): Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        nickname = parcel.readString(),
        model = parcel.readString() as String,
        serialNumber = parcel.readString(),
        categoryResId = parcel.readInt(),
        imageResId = parcel.readInt(),
        hasManual = parcel.readInt() == 1,
        hasMaintenanceGuides =  parcel.readInt() == 1,
        engineHours = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(nickname)
        parcel.writeString(model)
        parcel.writeString(serialNumber)
        parcel.writeInt(categoryResId)
        parcel.writeInt(imageResId)
        parcel.writeInt(if (hasManual) 1 else 0)
        parcel.writeInt(if (hasMaintenanceGuides) 1 else 0)
        parcel.writeInt(engineHours)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UIEquipment> {
        override fun createFromParcel(parcel: Parcel): UIEquipment {
            return UIEquipment(parcel)
        }

        override fun newArray(size: Int): Array<UIEquipment?> {
            return arrayOfNulls(size)
        }
    }
}