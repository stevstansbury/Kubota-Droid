//
//  EquipmentUnit.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.net.URL
import java.util.*

data class EquipmentUnit(
    val id: UUID,
    val model: String,
    val category: String?,
    val identifierType: String,
    val pinOrSerial: String?,
    val pin: String?,
    val serial: String?,
    val nickName: String?,
    val userEnteredEngineHours: Double?,
    val telematics: Telematics?,
    val guideUrl: URL?,
    val manualUrls: List<URL>?
)

val EquipmentUnit.manualInfo: List<ManualInfo>
    get() {
        return (manualUrls ?: emptyList()).map { it.manualInfo }
    }

