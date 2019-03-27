package com.kubota.network.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DealerResponse (val dealers: List<Dealer>?)

@JsonClass(generateAdapter = true)
data class Dealer (
    val id : String,
    val lastModified : String,
    val publicationDate : String,
    val dateCreated : String,
    val urlName : String,
    val serviceCertified : Boolean,
    val tier2Participant : Boolean,
    val rsmNumber : String,
    val productCodes : String,
    val dealerDivision : String,
    val address : Address,
    val phone : String,
    val dealerName : String,
    val dealerCertificationLevel : String,
    val dealerEmail : String,
    val rsmEmail : String,
    val districtNumber : String,
    val fax : String,
    val rsmName : String,
    val extendedWarranty : Boolean,
    val salesQuoteEmail : String,
    val dealerNumber : String,
    val distance : String = "")

@JsonClass(generateAdapter = true)
data class Address(
    val street: String,
    val city: String,
    val zip: String,
    val stateCode: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double)