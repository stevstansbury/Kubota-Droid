package com.android.kubota.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.kubota.utility.CategoryUtils
import com.android.kubota.utility.Utils
import com.kubota.repository.prefs.EquipmentPreferencesRepo

class EngineHoursViewModel(
    private val equipmentRepo: EquipmentPreferencesRepo,
    private val equipmentId: Int
): BaseEquipmentViewModel() {

    override val equipmentImage: LiveData<Int>

    override val equipmentModel: LiveData<String>

    val equipmentSerial: LiveData<String?>

    val equipmentNickname: LiveData<String?>

    val equipmentEngineHours: LiveData<Int>

    init {
        val equipmentLiveData = equipmentRepo.getEquipment(equipmentId = equipmentId)

        equipmentImage = MediatorLiveData<Int>().apply {
            addSource(equipmentLiveData) {
                value = if (it != null) CategoryUtils.getEquipmentImage(it.category, it.model) else 0
            }
        }
        equipmentModel = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.model
            }
        }
        equipmentSerial = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.serialNumber
            }
        }
        equipmentNickname = MediatorLiveData<String>().apply {
            addSource(equipmentLiveData) {
                value = it?.nickname
            }
        }
        equipmentEngineHours = MediatorLiveData<Int>().apply {
            addSource(equipmentLiveData) {
                value = it?.engineHours
            }
        }
    }

    fun updateEngineHours(engineHours: Int) {
        Utils.backgroundTask {
            equipmentRepo.updateEquipmentEngineHours(equipmentId, engineHours)
        }
    }
}