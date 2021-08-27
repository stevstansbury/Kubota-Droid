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


inline fun <T> List<T>.caseInsensitiveSort(crossinline selector: (T) -> kotlin.String?): List<T> =
    this.sortedWith(compareBy(java.lang.String.CASE_INSENSITIVE_ORDER, selector))

sealed class SearchModelType {
    data class PartialModelAndSerial(val partialModel: String, val serial: String): SearchModelType()
    data class PartialModelAndPIN(val partialModel: String, val pin: String): SearchModelType()
    data class PIN(val pin: String): SearchModelType()
}

interface EquipmentService {
    fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>>

    fun getModel(model: String): Promise<EquipmentModel?>

    fun searchModels(type: SearchModelType): Promise<List<EquipmentModel>>

    fun scanSearchModels(type: SearchModelType): Promise<List<String>>

    fun getModels(category: String): Promise<List<EquipmentModel>>

    fun getCategories(parentCategory: String? = null): Promise<List<EquipmentCategory>>

}
