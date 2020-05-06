//
//  KubotaEquipmentService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.service.CodableTypes
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.EquipmentService
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.EquipmentModels
import com.kubota.service.domain.FaultCode
import java.net.URL

internal data class ManualURL(
    val url: URL
)

internal class KubotaEquipmentService(config: Config): HTTPService(config = config), EquipmentService {

    override fun getFaultCodes(model: String, codes: List<String>): Promise<List<FaultCode>> {
        // We have to manually build the query string because the route has the form of
        // "/api/faultCode/{model}?code=1&code=5"
        // which the key name is not unique
        val query = if (codes.isEmpty()) "" else "?${codes.joinToString(separator = "&") { "code=${it}" }}"
        val criteria = CacheCriteria(policy = CachePolicy.useAgeReturnCacheIfError, age = CacheAge.oneDay.interval)
        return service {
            // The compiler has a little hard time to infer the type in this case, so we have
            // to help it out by declaring a local val with the type.
            val p: Promise<List<FaultCode>> = this.get(
                route = "/api/faultCode/${model}${query}",
                type = CodableTypes.newParameterizedType(List::class.java, FaultCode::class.java),
                cacheCriteria = criteria
            )
            return@service p
        }
    }

    override fun getManualURL(model: String): Promise<URL> {
        return service {
            val criteria = CacheCriteria(policy = CachePolicy.useAgeReturnCacheIfError, age = CacheAge.oneDay.interval)
            this.get(route = "/api/manuals/${model}", type = ManualURL::class.java, cacheCriteria = criteria).map { it.url }
        }
    }

    override fun getModel(model: String): Promise<EquipmentModel?> {
        TODO("Not yet implemented")
    }

    override fun getModels(): Promise<List<EquipmentModel>> {
        return service {
            val criteria = CacheCriteria(policy = CachePolicy.useAgeReturnCacheIfError, age = CacheAge.oneDay.interval)
            this.get(route = "/api/models", type = EquipmentModels::class.java).map { it.models }
        }
    }

}
