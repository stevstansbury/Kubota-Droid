//
//  InboxMessage.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize
import java.util.*

enum class InboxMessageSource {
    @Json(name="telematics")
    ALERTS,
    @Json(name="message")
    MESSAGES
}

@Parcelize
data class InboxMessage(
    val id: UUID,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val sourceFrom: InboxMessageSource,
    val createdTime: Date,
    val deepLink: Map<String, String> = emptyMap()
): Parcelable
