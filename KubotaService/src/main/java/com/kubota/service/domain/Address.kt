//
//  Address.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

data class Address(
    val street: String,
    val city: String,
    val zip: String,
    val stateCode: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val mapZoomLevel: Int?
)
