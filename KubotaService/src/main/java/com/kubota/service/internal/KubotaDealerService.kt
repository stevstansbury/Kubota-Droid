//
//  KubotaDealerService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.inmotionsoftware.foundation.service.CodableTypes
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.api.DealerService
import com.kubota.service.domain.Dealer

internal class KubotaDealerService(config: Config): HTTPService(config = config), DealerService {

    override fun getNearestDealers(latitude: Double, longitude: Double): Promise<List<Dealer>> {
        val params = mapOf(
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString()
        )
        return service {
            // The compiler has a little hard time to infer the type in this case, so we have
            // to help it out by declaring a local val with the type.
            val p: Promise<List<Dealer>> = this.get(
                route = "/api/dealer/nearest",
                query = params,
                type = CodableTypes.newParameterizedType(List::class.java, Dealer::class.java)
            )
            return@service p
        }
    }

}
