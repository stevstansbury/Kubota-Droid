package com.android.kubota.viewmodel

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.android.kubota.R
import com.android.kubota.extensions.toUIEquipment
import com.android.kubota.ui.action.UndoAction
import com.android.kubota.utility.Utils
import com.kubota.repository.data.Account
import com.kubota.repository.data.Equipment
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import com.kubota.repository.user.UserRepo
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

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

@Parcelize
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
    val engineHours: Int,
    val battery: Double?,
    val fuelLevel: Int?,
    val defLevel: Int?,
    private val engineState: Boolean?,
    val warnings: Int = 0
): Parcelable {

    @IgnoredOnParcel
    @DrawableRes
    val ignitionDrawable: Int = when(engineState) {
        true -> R.drawable.ic_ignition_on
        false -> R.drawable.ic_ignition_off
        else -> 0
    }

    @IgnoredOnParcel
    @DrawableRes
    val motionDrawableResId = when(val int = Random.nextInt(0, 3)) {
        1 -> R.drawable.ic_in_motion
        2 -> R.drawable.ic_in_transport
        3 -> R.drawable.ic_parked
        else -> 0
    }

    @IgnoredOnParcel
    val numberOfWarnings = Random.nextInt(0, 10)

    val errorMessage: String?
    get() {
        return if (numberOfWarnings > 0) {
            "E:9200 –mass air flow sensor failure. Contact your dealer."
        } else {
            null
        }
    }

}