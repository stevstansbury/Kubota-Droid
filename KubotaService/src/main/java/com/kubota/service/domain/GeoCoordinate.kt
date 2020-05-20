//
//  GeoCoordinate.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double
)
