//
//  KubotaBrowseService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.thenMap
import com.kubota.service.api.BrowseService
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentModels
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder

internal class KubotaBrowseService(private val couchbaseDb: Database?): BrowseService {

    override fun getRecentlyViewedEquipmentModels(): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.getRecentlyViewedModels()?.models ?: emptyList()
        }
    }

    override fun addRecentlyViewed(model: EquipmentModel): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.saveRecentlyViewed(model)?.models ?: emptyList()
        }
    }

    override fun removeRecentlyViewed(model: EquipmentModel): Promise<List<EquipmentModel>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.removeRecentlyViewed(model)?.models ?: emptyList()
        }
    }

}

@Throws
private fun Database.saveRecentlyViewed(model: EquipmentModel): EquipmentModels {
    val equipmentModels = this.getRecentlyViewedModels()?.models ?: emptyList()
    if (equipmentModels.firstOrNull { it.model == model.model } != null) return EquipmentModels(equipmentModels)

    val updateModels = EquipmentModels(arrayListOf(model).apply { addAll(equipmentModels) })
    this.saveRecentlyViewed(updateModels)
    return updateModels
}

@Throws
private fun Database.removeRecentlyViewed(model: EquipmentModel): EquipmentModels {
    val equipmentModels = this.getRecentlyViewedModels()?.models ?: emptyList()
    val updateModels = EquipmentModels(equipmentModels.filter { it.model != model.model })
    this.saveRecentlyViewed(updateModels)
    return updateModels
}

@Throws
private fun Database.saveRecentlyViewed(models: EquipmentModels) {
    val data = DictionaryEncoder().encode(models) ?: return
    val document = MutableDocument("RecentlyViewedEquipmentModels", data)
    this.save(document)
}

@Throws
private fun Database.getRecentlyViewedModels():EquipmentModels? {
    val document = this.getDocument("RecentlyViewedEquipmentModels") ?: return null
    val data = document.toMap()
    return DictionaryDeccoder().decode(type = EquipmentModels::class.java, value = data)
}
