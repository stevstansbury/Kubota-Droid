//
//  KubotaDealerService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.foundation.service.CodableTypes
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.recover
import com.inmotionsoftware.promisekt.then
import com.kubota.service.api.DealerService
import com.kubota.service.api.KubotaServiceError
import com.kubota.service.domain.Dealer
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder

internal class KubotaDealerService(config: Config, private val couchbaseDb: Database?): HTTPService(config = config), DealerService {

    override fun getNearestDealers(latitude: Double, longitude: Double): Promise<List<Dealer>> {
        val params = mapOf(
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString()
        )
        val p: Promise<List<Dealer>> = service {
            // The compiler has a little hard time to infer the type in this case, so we have
            // to help it out by declaring a local val with the type.
            val p1: Promise<List<Dealer>> = this.get(
                route = "/api/dealer/nearest",
                query = params,
                type = CodableTypes.newParameterizedType(List::class.java, Dealer::class.java)
            )
            return@service p1
        }
        return p.then(on = DispatchExecutor.global) { dealers ->
            try { this.couchbaseDb?.saveNearestDealers(dealers) } catch (error: Throwable) {}
            Promise.value(dealers)
        }
        .recover(on = DispatchExecutor.global) {err ->
            val error = err as? KubotaServiceError ?: throw err
            when (error) {
                is KubotaServiceError.Unauthorized -> throw error
                else -> {
                    val dealers = this.couchbaseDb?.getNearestDealers() ?: throw error
                    Promise.value(dealers)
                }
            }
        }
    }

}

private data class NearestDealers(val dealers: List<Dealer>)

@Throws
private fun Database.saveNearestDealers(dealers: List<Dealer>) {
    val data = DictionaryEncoder().encode(NearestDealers(dealers = dealers)) ?: return
    val document = MutableDocument("NearestDealers", data)
    this.save(document)
}

@Throws
private fun Database.getNearestDealers(): List<Dealer>? {
    val document = this.getDocument("NearestDealers") ?: return null
    val data = document.toMap()
    val nearest = DictionaryDeccoder().decode(type = NearestDealers::class.java, value = data)
    return nearest?.dealers
}
