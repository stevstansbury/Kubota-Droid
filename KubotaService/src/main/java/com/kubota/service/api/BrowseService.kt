//
//  BrowseService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentModel
import com.kubota.service.domain.RecentViewedItem

interface BrowseService {

    fun getRecentViewedItems(): Promise<List<RecentViewedItem>>

    fun addRecentViewedItem(item: RecentViewedItem, limitTo: Int): Promise<List<RecentViewedItem>>

    fun removeRecentViewedItem(item: RecentViewedItem): Promise<List<RecentViewedItem>>

    fun clearBrowseHistory(): Promise<Unit>

}
