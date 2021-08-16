//
//  EquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentCategory
import com.kubota.service.domain.EquipmentMaintenance
import com.kubota.service.domain.EquipmentModel

inline fun <T> List<T>.caseInsensitiveSort(crossinline selector: (T) -> String?): List<T> =
    this.sortedWith(compareBy(java.lang.String.CASE_INSENSITIVE_ORDER, selector))

sealed class SearchModelType {
    data class PartialModelAndSerial(val partialModel: String, val serial: String) : SearchModelType()
    data class PartialModelAndPIN(val partialModel: String, val pin: String) : SearchModelType()
    data class PIN(val pin: String) : SearchModelType()
}

sealed class EquipmentModelTree {
    data class Category(val category: EquipmentCategory, val items: List<EquipmentModelTree>) : EquipmentModelTree()
    data class Model(val model: EquipmentModel) : EquipmentModelTree()
}

interface EquipmentService {
    fun getMaintenanceSchedule(model: String): Promise<List<EquipmentMaintenance>>

    fun getModel(model: String): Promise<EquipmentModel?>

    fun searchModels(type: SearchModelType): Promise<List<EquipmentModel>>

    fun scanSearchModels(type: SearchModelType): Promise<List<String>>

    fun getModels(category: String): Promise<List<EquipmentModel>>

    fun getCategories(parentCategory: String? = null): Promise<List<EquipmentCategory>>

    /**
     * getEquipmentTree will return the entire tree if no filters are applied,
     * and will prune branches/nodes that do not fit the filter criteria.
     *
     * Will always return the full height of the tree, but with enough filters
     * it will look more like a linked list
     */
    fun getEquipmentTree(
        modelFilters: List<String>,
        categoryFilters: List<String>
    ): Promise<List<EquipmentModelTree>>

    fun getCompatibleMachines(model: String): Promise<List<EquipmentModel>>

    fun getAvailableModels(): Promise<List<EquipmentModel>>
}
