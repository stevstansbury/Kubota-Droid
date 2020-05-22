//
//  KubotaBrowseService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.internal

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.inmotionsoftware.foundation.concurrent.DispatchExecutor
import com.inmotionsoftware.promisekt.Promise
import com.inmotionsoftware.promisekt.map
import com.kubota.service.api.BrowseService
import com.kubota.service.domain.RecentViewedItem
import com.kubota.service.domain.RecentViewedItems
import com.kubota.service.internal.couchbase.DictionaryDeccoder
import com.kubota.service.internal.couchbase.DictionaryEncoder

internal class KubotaBrowseService(private val couchbaseDb: Database?): BrowseService {

    override fun getRecentViewedItems(): Promise<List<RecentViewedItem>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.getRecentViewedItems() ?: emptyList()
        }
    }

    override fun addRecentViewedItem(item: RecentViewedItem, limitTo: Int): Promise<List<RecentViewedItem>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.saveRecentViewed(item, limitTo) ?: emptyList()
        }
    }

    override fun removeRecentViewedItem(item: RecentViewedItem): Promise<List<RecentViewedItem>> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.removeRecentViewed(item) ?: emptyList()
        }
    }

    override fun clearBrowseHistory(): Promise<Unit> {
        return Promise.value(Unit).map(on = DispatchExecutor.global) {
            this.couchbaseDb?.clearRecentViews() ?: Unit
        }
    }

}

@Throws
private fun Database.saveRecentViewed(item: RecentViewedItem, limitTo: Int): List<RecentViewedItem> {
    // First remove if exist
    val viewedItems = this.removeRecentViewed(item)

    // Then add in the new item
    val limitSize = limitTo.minus(1)
    val items = if (viewedItems.size > limitSize) {
        viewedItems.subList(0, limitSize)
    } else {
        viewedItems
    }

    val newItems = RecentViewedItems(arrayListOf(item).apply { addAll(items) })
    this.saveRecentViewed(newItems)
    return newItems.items
}

@Throws
private fun Database.removeRecentViewed(item: RecentViewedItem): List<RecentViewedItem> {
    val items = this.getRecentViewedItems() ?: emptyList()
    val remaining = RecentViewedItems(items.filter { it.id != item.id })
    this.saveRecentViewed(remaining)
    return remaining.items
}

@Throws
private fun Database.clearRecentViews() {
    this.saveRecentViewed(RecentViewedItems(emptyList()))
}

@Throws
private fun Database.saveRecentViewed(items: RecentViewedItems) {
    val data = DictionaryEncoder().encode(items) ?: return
    val document = MutableDocument("RecentViewedItems", data)
    this.save(document)
}

@Throws
private fun Database.getRecentViewedItems(): List<RecentViewedItem>? {
    val document = this.getDocument("RecentViewedItems") ?: return null
    return DictionaryDeccoder().decode(
                type = RecentViewedItems::class.java,
                value = document.toMap()
            )?.items?.sortedByDescending { it.viewedDate }
}
