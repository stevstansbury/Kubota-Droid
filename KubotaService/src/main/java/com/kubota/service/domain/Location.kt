//
//  Location.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

data class Location(
    val x: Double,
    val y: Double,
    val coordinates: List<Double>,
    val type: String
)
