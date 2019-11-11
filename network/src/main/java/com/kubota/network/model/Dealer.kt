package com.kubota.network.model

import com.squareup.moshi.JsonClass

private const val EMPTY_STRING = ""
private const val DEFAULT_BOOLEAN = false

@JsonClass(generateAdapter = true)
data class Dealer (
    val id : String,
    val lastModified : String = EMPTY_STRING,
    val publicationDate : String = EMPTY_STRING,
    val expirationDate: String? = null,
    val dateCreated : String = EMPTY_STRING,
    val urlName : String,
    val serviceCertified : Boolean = DEFAULT_BOOLEAN,
    val tier2Participant : Boolean = DEFAULT_BOOLEAN,
    val rsmNumber : String = EMPTY_STRING,
    val productCodes : String = EMPTY_STRING,
    val dealerDivision : String = EMPTY_STRING,
    val address : Address,
    val phone : String,
    val dealerName : String,
    val dealerCertificationLevel : String = EMPTY_STRING,
    val dealerEmail : String = EMPTY_STRING,
    val rsmEmail : String = EMPTY_STRING,
    val districtNumber : String = EMPTY_STRING,
    val fax : String = EMPTY_STRING,
    val rsmName : String = EMPTY_STRING,
    val extendedWarranty : Boolean = DEFAULT_BOOLEAN,
    val salesQuoteEmail : String = EMPTY_STRING,
    val dealerNumber : String,
    val distance : String = "")

@JsonClass(generateAdapter = true)
data class Address(
    val id: String = EMPTY_STRING,
    val street: String,
    val city: String,
    val zip: String,
    val stateCode: String,
    val countryCode: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mapZoomLevel: Int = 18)