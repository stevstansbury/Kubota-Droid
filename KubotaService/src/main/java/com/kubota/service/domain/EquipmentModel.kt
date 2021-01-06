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

@Parcelize
data class EquipmentModel(
    val model: String,
    val searchModel: String?,
    val description: String?,
    val imageResources: ImageResources?,
    val category: String,
    val subcategory: String,
    val guideUrl: URL?,
    val manualInfo: List<ManualInfo>,
    val warrantyUrl: URL?,
    val hasFaultCodes: Boolean,
    val hasMaintenanceSchedules: Boolean
): Parcelable
