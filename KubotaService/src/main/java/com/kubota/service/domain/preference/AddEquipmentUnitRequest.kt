//
//  AddEquipmentUnitRequest.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain.preference

enum class EquipmentUnitIdentifier {
    Pin,
    Serial
}

data class AddEquipmentUnitRequest(
    val identifierType: EquipmentUnitIdentifier,
    val pinOrSerial: String?,
    val model: String,
    val nickName: String?,
    val engineHours: Double
)
