package com.kubota.service.domain

import java.util.*

data class RecentViewedItem(
    val id: String,
    val type: String,
    val viewedDate: Date,
    val title: String? = null,
    val metadata: Map<String, String>? = null
)

internal class RecentViewedItems(
    val items: List<RecentViewedItem>
)
