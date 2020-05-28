//
//  EquipmentModel.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import java.net.URL

internal data class EquipmentModels(
    val models: List<EquipmentModel>
)

data class EquipmentModel(
    val model: String,
    val category: String,
    val guideUrl: URL?,
    val manualUrls: List<URL>?
)

val EquipmentModel.manualInfo: List<ManualInfo>
    get() {
        return (this.manualUrls ?: emptyList()).map { it.manualInfo }
    }
