package com.kubota.service.domain

//
//  VideoInfo.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//


import java.net.URL
import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

@Parcelize data class VideoInfo(
        @Json(name = "name")
        val title: String,
        val url: URL
): Parcelable
