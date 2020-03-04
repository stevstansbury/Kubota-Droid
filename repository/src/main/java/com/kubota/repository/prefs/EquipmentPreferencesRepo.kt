package com.kubota.repository.prefs

import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Equipment
import com.kubota.repository.data.EquipmentDao

class EquipmentPreferencesRepo(private val equipmentDao: EquipmentDao) {

    companion object {
        internal const val EXTRA_EQUIPMENT = "equipment"
    }

    fun getSavedEquipments() = equipmentDao.getLiveDataEquipments()

    fun insertEquipment(equipment: Equipment) {
        BaseApplication.serviceProxy.addEquipment(equipment)
    }

    fun deleteEquipment(equipment: Equipment) {
        BaseApplication.serviceProxy.deleteEquipment(equipment)
    }

    fun getEquipment(equipmentId: Int) = equipmentDao.getLiveDataEquipment(equipmentId)

    fun updateEquipment(equipmentId: Int, serialNumber: String?) {
        equipmentDao.getEquipment(equipmentId)
            ?.copy(serialNumber = serialNumber)
            ?.let { BaseApplication.serviceProxy.updateEquipment(it) }
    }

    fun updateEquipmentEngineHours(equipmentId: Int, engineHours: Int) {
        equipmentDao.getEquipment(equipmentId)
            ?.copy(engineHours = engineHours)
            ?.let { BaseApplication.serviceProxy.updateEquipment(it) }
    }
}