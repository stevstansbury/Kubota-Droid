//
//  FaultCode.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FaultCode (
    val code: Int?,
    val j1939Spn: Int?,
    val j1939Fmi: Int?,
    val errorType: String,
    val description: String,
    val accelerationLimited: String?,
    val engineOutputLimited: String?,
    val engineStopped: String?,
    val machinePerformance: String?,
    val provisionalMeasure: String?,
    val dealerTitle: String?,
    val customerTitle: String?,
    val dealerMessage: String?,
    val customerMessage: String?,
    val timeReported: Date?
): Parcelable
