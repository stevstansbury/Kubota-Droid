//
//  ImageResources.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.net.URL

@Parcelize
data class ImageResources(
    val heroUrl: URL?,
    val fullUrl: URL?,
    val iconUrl: URL?
): Parcelable
