package com.kubota.repository.service

import androidx.annotation.WorkerThread

val MOCK_DATA = listOf(
    ServiceInterval(300, listOf(
        "Maintenance Checks",
        "Transmission Fluid Change",
        "Hydraulic Fluid Change"
    )),
    ServiceInterval(400, listOf(
        "Maintenance Checks",
        "Transmission Fluid Change",
        "Hydraulic Fluid Change",
        "Replace Crank Case",
        "Cylinder Head Change",
        "Main Bearing Case Maintenance Check",
        "Flywheel Change",
        "Injection Pump Change",
        "Idle Apparatus Change",
        "Nozzle Holder and Glow Plug Change"
    )),
    ServiceInterval(500, listOf(
        "Maintenance Checks",
        "Transmission Fluid Change",
        "Hydraulic Fluid Change"
    ))
)

class EquipmentMaintenanceService {

    @WorkerThread
    fun getMaintenanceSchedule(equipmentCategory: String, equipmentModel: String): ServiceResponse {
        return ServiceResponse.Success(MOCK_DATA)
    }
}

data class ServiceInterval (
    val engineHours: Int,
    val maintenance: List<String>
)

sealed class ServiceResponse {
    class Success(val maintenanceService: List<ServiceInterval>): ServiceResponse()
    class GenericError: ServiceResponse()
    class IOError: ServiceResponse()
}