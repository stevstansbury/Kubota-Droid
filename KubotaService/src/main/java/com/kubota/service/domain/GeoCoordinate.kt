//
//  GeoCoordinate.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val positionHeadingAngle: Int?
): Parcelable
