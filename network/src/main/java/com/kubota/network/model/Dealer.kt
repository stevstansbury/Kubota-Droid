package com.kubota.network.model

import com.squareup.moshi.JsonClass

private const val EMPTY_STRING = ""
private const val DEFAULT_BOOLEAN = false

@JsonClass(generateAdapter = true)
data class Dealer (
    val id : String,
    val urlName : String,
    val address : Address,
    val phone : String,
    val dealerName : String,
    val dealerEmail : String = EMPTY_STRING,
    val dealerNumber : String,
    val distance : String = "")

@JsonClass(generateAdapter = true)
data class Address(
    val street: String,
    val city: String,
    val zip: String,
    val stateCode: String,
    val countryCode: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0)