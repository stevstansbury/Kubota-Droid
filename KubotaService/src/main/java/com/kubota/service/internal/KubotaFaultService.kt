package com.kubota.service.internal

import com.inmotionsoftware.foundation.cache.CacheAge
import com.inmotionsoftware.foundation.cache.CacheCriteria
import com.inmotionsoftware.foundation.cache.CachePolicy
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.get
import com.inmotionsoftware.foundation.service.queryParams
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.inmotionsoftware.promisekt.recover
import com.kubota.service.api.FaultService
import com.kubota.service.api.SearchFaultCode
import com.kubota.service.domain.FaultCode
import java.util.*

private data class FaultCodes(
    val faultCodes: List<FaultCode>
)

class KubotaFaultService(config: Config) : HTTPService(config = config), FaultService {

    override fun searchFaultCodes(searchType: SearchFaultCode): Promise<List<FaultCode>> {
        val params = queryParams(
            "code" to when (searchType) {
                is SearchFaultCode.All -> searchType.code
                is SearchFaultCode.Dtc -> searchType.code
                is SearchFaultCode.J1939 -> "${(searchType.spn ?: "*")}/${(searchType.fmi ?: "*")}"
            },
            "errorType" to when (searchType) {
                is SearchFaultCode.All -> "all"
                is SearchFaultCode.Dtc -> "dtc"
                is SearchFaultCode.J1939 -> "j1939"
            }
        )
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneDay.interval)
        return service {
            this.get(
                route = "/api/faultCode/${searchType.model}",
                query = params,
                type = FaultCodes::class.java,
                cacheCriteria = criteria
            ).map { it.faultCodes }
        }.recover {
            when (it.message?.contains("fault code info not found")) {
                true -> Promise.value(emptyList())
                else -> throw it
            }
        }
    }

    override fun getRecentCodes(equipmentId: UUID): Promise<List<FaultCode>> {
        val criteria = CacheCriteria(policy = CachePolicy.useAge, age = CacheAge.oneMinute.interval)
        return service {
            this.get(
                route = "/api/user/equipment/$equipmentId/recentcodes",
                type = FaultCodes::class.java,
                cacheCriteria = criteria
            ).map { it.faultCodes }
        }
    }
}