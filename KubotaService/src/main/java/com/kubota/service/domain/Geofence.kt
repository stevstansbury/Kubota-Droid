package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Geofence(
    val uuid: UUID = UUID.randomUUID(),
    val name: String = "",
    val points: List<GeoCoordinate> = mutableListOf()
): Parcelable