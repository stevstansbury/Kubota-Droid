//
//  BrowseService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.EquipmentModel

interface BrowseService {

    fun getRecentlyViewedEquipmentModels(): Promise<List<EquipmentModel>>

    fun addRecentlyViewed(model: EquipmentModel): Promise<List<EquipmentModel>>

    fun removeRecentlyViewed(model: EquipmentModel): Promise<List<EquipmentModel>>

}
