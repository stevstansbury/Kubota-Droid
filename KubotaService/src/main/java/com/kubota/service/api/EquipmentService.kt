//
//  EquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.FaultCode
import com.kubota.service.domain.EquipmentMaintenance


sealed class SearchModelType {
    data class PartialModelAndSerial(val partialModel: String, val serial: String): SearchModelType()
    data class PartialModelAndPIN(val partialModel: String, val pin: String): SearchModelType()
    data class PIN(val pin: String): SearchModelType()
}

interface EquipmentService {

    fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>>

    fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>>

    fun getModel(model: String): Promise<EquipmentModel?>

    fun searchModels(type: SearchModelType): Promise<List<EquipmentModel>>

    fun getModels(category: String): Promise<List<EquipmentModel>>

    fun getCategories(parentCategory: String? = null): Promise<List<EquipmentCategory>>

}
