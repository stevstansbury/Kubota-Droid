//
//  EquipmentModel.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.net.URI

internal data class EquipmentModels(
    val models: List<EquipmentModel>
)

data class EquipmentModel(
    val model: String,
    val category: String,
    val guideUrl: URI?
)
