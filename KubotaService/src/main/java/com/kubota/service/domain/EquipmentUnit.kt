//
//  EquipmentUnit.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.net.URL
import java.util.*

@Parcelize
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
    val modelHeroUrl: String?,
    val modelFullUrl: String?,
    val modelIconUrl: String?,
    val guideUrl: URL?,
    val manualUrls: List<URL>?
): Parcelable

val EquipmentUnit.displayName: String
    get() = if (nickName.isNullOrBlank()) this.model else nickName

val EquipmentUnit.manualInfo: List<ManualInfo>
    get() {
        return (manualUrls ?: emptyList()).map { it.manualInfo }
    }

