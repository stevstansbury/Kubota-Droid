//
//  FaultCode.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

data class FaultCode (
    val code: Int,
    val description: String,
    val accelerationLimited: String?,
    val engineOutputLimited: String?,
    val engineStopped: String?,
    val machinePerformance: String?,
    val provisionalMeasure: String?,
    val dealerTitle: String,
    val customerTitle: String,
    val dealerMessage: String,
    val customerMessage: String
)
