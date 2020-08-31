//
//  RestartInhibitStatus.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

enum class RestartInhibitStatusCode {
    RestartEnabled,
    RestartDisabled
}

@Parcelize
data class RestartInhibitStatus(
    val canModify: Boolean,
    val commandStatus: RestartInhibitStatusCode,
    val equipmentStatus: RestartInhibitStatusCode
): Parcelable
