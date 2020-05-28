//
//  KubotaEquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.*
import com.inmotionsoftware.promisekt.*
import com.kubota.service.api.EquipmentService
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.*
import com.kubota.service.domain.EquipmentModels
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder
import java.net.URL

internal data class ManualURLs(
    val manualUrls: List<URL>
)

private data class FaultCodes(
    val faultCodes: List<FaultCode>
)

internal class KubotaEquipmentService(config: Config, private val couchbaseDb: Database?): HTTPService(config = config), EquipmentService {

    override fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>> {
        val params = queryParamsMultiValue(
            "code" to codes
        )
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneDay.interval)
        return service {
            // The compiler has a little hard time to infer the type in this case, so we have
            // to help it out by declaring a local val with the type.
            val p: Promise<FaultCodes> = this.get(
                route = "/api/faultCode/${model}",
                query = params,
                type = FaultCodes::class.java,
                cacheCriteria = criteria
            )
            return@service p.map { it.faultCodes }
        }
    }

    override fun getManualInfo(model: String): Promise<List<ManualInfo>> {
        return service {
            val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneDay.interval * 7)
            this.get(route = "/api/manuals/${model}", type = ManualURLs::class.java, cacheCriteria = criteria)
                .map { result -> result.manualUrls.map { it.manualInfo } }
        }
    }

    override fun getModel(model: String): Promise<EquipmentModel?> {
        return Promise.value(Unit).thenMap(on = DispatchExecutor.global) {
            val models = this.couchbaseDb?.getEquipmentModels()?.models
            if (models != null && !models.isEmpty()) {
                Promise.value(models)
            } else {
                this.getModels()
            }
        }.map { models ->
            models.firstOrNull { it.model == model }
        }
    }

    override fun getModels(): Promise<List<EquipmentModel>> {
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneDay.interval)
        val p: Promise<EquipmentModels> = service {
            this.get(route = "/api/models", type = EquipmentModels::class.java, cacheCriteria = criteria)
        }
        return p.then(on = DispatchExecutor.global) {models ->
            this.couchbaseDb?.saveEquipmentModels(models)
            Promise.value(models)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val models = this.couchbaseDb?.getEquipmentModels() ?: throw error
                    Promise.value(models)
                }
            }
        }
        .map {
            it.models
        }
    }

    override fun searchModels(partialModel: String, serial: String): Promise<List<EquipmentModel>> {
        val params = queryParams(
            "partialModel" to partialModel,
            "serial" to serial
        )
        return service {
            this.get(route = "/api/models", query = params, type = EquipmentModels::class.java).map { it.models }
        }
    }

    override fun getModels(category: String): Promise<List<EquipmentModel>> {
        TODO("To implement when server API is available")
    }

    override fun getCategories(parentCategory: String?): Promise<List<EquipmentCategory>> {
        TODO("To implement when server API is available")
    }

}

@Throws
private fun Database.saveEquipmentModels(models: EquipmentModels) {
    val data = DictionaryEncoder().encode(models) ?: return
    val document = MutableDocument("EquipmentModels", data)
    this.save(document)
}

@Throws
private fun Database.getEquipmentModels(): EquipmentModels? {
    val document = this.getDocument("EquipmentModels") ?: return null
    val data = document.toMap()
    return DictionaryDeccoder().decode(type = EquipmentModels::class.java, value = data)
}
