package com.kubota.repository.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.kubota.repository.BaseApplication
import com.kubota.repository.data.Equipment
import com.kubota.repository.data.EquipmentDao
import com.kubota.repository.data.FaultCodeDao
import com.kubota.repository.uimodel.Equipment as UIEquipment
import com.kubota.repository.uimodel.GeoLocation
import com.kubota.repository.uimodel.Telematics
import com.kubota.repository.utils.Function2

class EquipmentPreferencesRepo(
    private val equipmentDao: EquipmentDao,
    private val faultCodeDao: FaultCodeDao
) {

    companion object {
        internal const val EXTRA_EQUIPMENT = "equipment"
    }

    fun getSavedEquipments() = equipmentDao.getLiveDataEquipments()

    fun getSavedEquipment(id: Int): LiveData<UIEquipment?> {
        val equipmentLiveData = equipmentDao.getLiveDataEquipment(id)
        val faultCodesLiveData = Transformations.map(faultCodeDao.getLiveDataFaultCodes(id)) {
            return@map !it.isNullOrEmpty()
        }

        val func = object : Function2<Equipment?, Boolean, UIEquipment?> {
            override fun apply(
                input1: Equipment?,
                input2: Boolean
            ): UIEquipment? {
                return input1?.let {
                    UIEquipment(
                        id,
                        it.nickname,
                        it.category,
                        it.model,
                        it.serialNumber,
                        it.manualLocation != null,
                        it.hasGuide,
                        it.engineHours,
                        it.getTelematicsData(hasFaultCodes = input2)
                    )
                }
            }
        }

        val results = MediatorLiveData<UIEquipment?>()

        results.addSource(equipmentLiveData) {
            results.postValue(func.apply(equipmentLiveData.value, faultCodesLiveData.value ?: false))
        }
        results.addSource(faultCodesLiveData) {
            results.postValue(func.apply(equipmentLiveData.value, faultCodesLiveData.value ?: false))
        }

        return results
    }

    fun insertEquipment(equipment: Equipment) {
        BaseApplication.serviceProxy.addEquipment(equipment)
    }

    fun deleteEquipment(equipment: Equipment) {
        BaseApplication.serviceProxy.deleteEquipment(equipment)
    }

    fun getEquipment(equipmentId: Int) = equipmentDao.getLiveDataEquipment(equipmentId)

    fun updateEquipmentSerialPin(equipmentId: Int, serialNumber: String?) {
        equipmentDao.getEquipment(equipmentId)
            ?.copy(serialNumber = serialNumber)
            ?.let { BaseApplication.serviceProxy.updateEquipment(it) }
    }

    fun updateEquipmentNickName(equipmentId: Int, serialNumber: String?) {
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

private fun Equipment.getTelematicsData(hasFaultCodes: Boolean): Telematics? {
    if (engineState != null || battery != null || fuelLevel != null || defLevel != null ||
        latitude != null || longitude != null) {
        val location = if (latitude != null && longitude != null)
            GeoLocation(latitude, longitude)
        else
            null

        return Telematics(
            engineStatus = engineState,
            batteryVoltage = battery,
            fuelLevel = fuelLevel?.let { it/100.00 },
            defLevel = defLevel?.let { it/100.00 },
            location = location,
            hasFaultCodes = hasFaultCodes
        )
    }

    return null
}