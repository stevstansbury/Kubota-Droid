package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.kubota.extensions.toUIEquipment
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Account
import com.kubota.repository.data.Equipment
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.user.UserRepo

class MyEquipmentViewModel(override val userRepo: UserRepo, private val equipmentPrefsRepo: EquipmentPreferencesRepo) : ViewModel(), LoggedIn  by LoggedInDelegate(userRepo) {
    private var equipmentList = emptyList<Equipment>()

    val isLoading: LiveData<Boolean> = Transformations.map(userRepo.getAccount()) {
        return@map it?.flags == Account.FLAGS_SYNCING
    }

    val preferenceEquipmentList = Transformations.map(equipmentPrefsRepo.getSavedEquipments()) { equipmentList ->
        val results = mutableListOf<UIEquipment>()
        this.equipmentList = equipmentList ?: emptyList()
        equipmentList?.forEach {
            results.add(it.toUIEquipment())
        }

        return@map results
    }

    fun createDeleteAction(equipment: UIEquipment): UndoAction {
        val repoEquipment = equipmentList.find { m -> equipment.id == m.id}
        return object : UndoAction {

            override fun commit() {
                repoEquipment?.let {
                    Utils.backgroundTask { equipmentPrefsRepo.deleteEquipment(repoEquipment) }
                }
            }

            override fun undo() {
                repoEquipment?.let {
                    Utils.backgroundTask { equipmentPrefsRepo.insertEquipment(repoEquipment) }
                }
            }
        }
    }

    fun createMultiDeleteAction(equipment: List<UIEquipment>): UndoAction {
        return object : UndoAction{
            private var deleteList = mutableListOf<Equipment>()
            override fun commit() {
                //for each UIEquipment, check if there is a matching Equipment
                equipment.forEach { equipment->
                    val item = equipmentList.find { m -> equipment.id == m.id}
                    item?.let {
                        deleteList.add(it)
                    }
                }
                Utils.backgroundTask {
                    deleteList.forEach{equipment->
                        equipmentPrefsRepo.deleteEquipment(equipment)
                    }
                }
            }

            override fun undo() {
                Utils.backgroundTask {
                    deleteList.forEach { equipment->
                        equipmentPrefsRepo.insertEquipment(equipment)
                    }
                }
            }
        }
    }

    fun getUpdatedEquipmentList(){
        userRepo.syncAccount()
    }
}

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