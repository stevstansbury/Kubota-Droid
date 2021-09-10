package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.FaultCode
import java.util.*


sealed class SearchFaultCode(open val model: String) {
    data class All(override val model: String, val code: String): SearchFaultCode(model)
    data class Dtc(override val model: String, val code: String): SearchFaultCode(model)
    data class J1939(override val model: String, val spn: String?, val fmi: String?): SearchFaultCode(model)
}

interface FaultService {
    fun searchFaultCodes(searchType: SearchFaultCode): Promise<List<FaultCode>>

    fun getRecentCodes(equipmentId: UUID): Promise<List<FaultCode>>
}