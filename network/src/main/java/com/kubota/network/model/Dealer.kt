package com.kubota.network.model

import com.google.gson.annotations.SerializedName

class Dealer() {
    @SerializedName("id")
    var id : String = ""

    @SerializedName("lastModified")
    var lastModified : String = ""

    @SerializedName("publicationDate")
    var publicationDate : String = ""

    @SerializedName("dateCreated")
    var dateCreated : String = ""

    @SerializedName("urlName")
    var urlName : String = ""

    @SerializedName("serviceCertified")
    var serviceCertified : Boolean = false

    @SerializedName("tier2Participant")
    var tier2Participant : Boolean = false

    @SerializedName("rsmNumber")
    var rsmNumber : String = ""

    @SerializedName("productCodes")
    var productCodes : String = ""

    @SerializedName("dealerDivision")
    var dealerDivision : String = ""

    @SerializedName("address")
    var address : Address = Address()

    @SerializedName("phone")
    var phone : String = ""

    @SerializedName("dealerName")
    var dealerName : String = ""

    @SerializedName("dealerCertificationLevel")
    var dealerCertificationLevel : String = ""

    @SerializedName("dealerEmail")
    var dealerEmail : String = ""

    @SerializedName("rsmEmail")
    var rsmEmail : String = ""

    @SerializedName("districtNumber")
    var districtNumber : String = ""

    @SerializedName("fax")
    var fax : String = ""

    @SerializedName("rsmName")
    var rsmName : String = ""

    @SerializedName("extendedWarranty")
    var extendedWarranty : Boolean = false

    @SerializedName("salesQuoteEmail")
    var salesQuoteEmail : String = ""

    @SerializedName("dealerNumber")
    var dealerNumber : String = ""

    @SerializedName("distance")
    var distance : String = ""
}

class Address() {
    @SerializedName("id")
    var id : String = ""

    @SerializedName("street")
    var street: String = ""

    @SerializedName("city")
    var city: String = ""

    @SerializedName("zip")
    var zip: String = ""

    @SerializedName("stateCode")
    var stateCode: String = ""

    @SerializedName("countryCode")
    var countryCode: String = ""

    @SerializedName("latitude")
    var latitude: Double = 0.0

    @SerializedName("longitude")
    var longitude: Double = 0.0
}