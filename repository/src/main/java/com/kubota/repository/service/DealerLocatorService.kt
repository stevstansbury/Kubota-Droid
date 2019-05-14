package com.kubota.repository.service

import com.kubota.network.model.Dealer
import com.kubota.network.service.DealerLocatorAPI
import com.kubota.network.service.NetworkResponse

class DealerLocatorService {

    private val api = DealerLocatorAPI()

    fun searchDealers(latitude: Double, longitude: Double): List<SearchDealer> {
        val results = api.getDealers(latitude = latitude, longitude = longitude)
        return when (results) {
            is NetworkResponse.Success -> results.value.map { it.toSearchDealer() }
            is NetworkResponse.IOException -> emptyList()
            is NetworkResponse.ServerError -> emptyList()
        }
    }
}

data class SearchDealer(val serverId : String, val name : String, val streetAddress: String, val city: String,
    val stateCode: String, val postalCode: String, val countryCode: String, val phone : String,
    val webAddress : String, val dealerNumber : String, val latitude: Double, val longitude: Double,
    val distance : String)

private fun Dealer.toSearchDealer(): SearchDealer {
    return SearchDealer(serverId = id, name = dealerName, streetAddress = address.street, city = address.city,
        stateCode = address.stateCode, postalCode = address.zip, countryCode = address.countryCode, phone = phone,
        webAddress = urlName, dealerNumber = dealerNumber, latitude = address.latitude, longitude = address.longitude,
        distance = distance)
}