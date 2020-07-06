package com.kubota.service.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

enum class NotificationSource(name: String) {
    ALERTS("telematics"),
    MESSAGES("message")
}

@Parcelize
data class Notification(
    val id: UUID,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val sourceFrom: NotificationSource,
    val createdTime: String
): Parcelable