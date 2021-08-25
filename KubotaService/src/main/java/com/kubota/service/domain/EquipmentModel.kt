//
//  EquipmentModel.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import com.inmotionsoftware.foundation.service.JSONAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.android.parcel.Parcelize
import java.net.URL
import java.util.*

@Parcelize
data class EquipmentModel(
    val model: String,
    val searchModel: String?,
    val type: Type,
    val description: String?,
    val imageResources: ImageResources?,
    val category: String,
    val guideUrl: URL?,
    val manualEntries: List<ManualInfo>,
    val videoEntries: List<VideoInfo>,
    val warrantyUrl: URL?,
    val hasFaultCodes: Boolean,
    val hasMaintenanceSchedules: Boolean,
    val compatibleAttachments: List<String>,
    val discontinuedDate: Date?
): Parcelable {
    enum class Type {
        Machine,
        Attachment
    }
}

class ModelTypeJsonAdapter: JSONAdapter {
    @FromJson
    fun fromJson(type: String?): EquipmentModel.Type? {
        return type?.let {
            EquipmentModel.Type.values().first {
                it.name.lowercase() == type.lowercase()
            }
        }
    }

    @ToJson
    fun toJson(value: EquipmentModel.Type?): String? {
        return value?.toString()?.lowercase()
    }
}
