//
//  EquipmentModel.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.net.URL

internal data class EquipmentModels(
    val models: List<EquipmentModel>
)

@Parcelize
data class EquipmentModel(
    val model: String,
    val category: String,
    val guideUrl: URL?,
    val manualUrls: List<URL>?
): Parcelable

val EquipmentModel.manualInfo: List<ManualInfo>
    get() {
        return (this.manualUrls ?: emptyList()).map { it.manualInfo }
    }
