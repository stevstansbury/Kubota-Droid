package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Geofence(
    val id: Int = 0,
    val description: String = "",
    val points: List<GeoCoordinate> = mutableListOf()
): Parcelable