//
//  Location.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import com.squareup.moshi.Json

data class Location(
    @Json(name = "x")
    val longitude: Double,
    @Json(name = "y")
    val latitude: Double,
    val coordinates: List<Double>,
    val type: String
)
