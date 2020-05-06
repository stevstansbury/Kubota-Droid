//
//  Dealer.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.util.*

data class Dealer (
    val id : UUID,
    val address: Address,
    val dateCreated: Double,
    val dealerCertificationLevel: String,
    val dealerDivision: String,
    val dealerEmail: String,
    val dealerName: String,
    val dealerNumber: String,
    val distance: Double?,
    val expirationDate: Double?,
    val extendedWarranty: Boolean,
    val fax: String,
    val lastModified: Date,
    val location: Location,
    val phone: String,
    val productCodes: String,
    val publicationDate: Date,
    val salesQuoteEmail: String,
    val serviceCertified: Boolean,
    val tier2Participant: Boolean,
    val urlName: String,
    val rsmemail: String,
    val rsmname: String,
    val rsmnumber: String
)
