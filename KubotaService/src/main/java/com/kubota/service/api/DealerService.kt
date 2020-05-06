//
//  DealerService.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

import com.inmotionsoftware.promisekt.Promise
import com.kubota.service.domain.Dealer

interface DealerService {

    fun getNearestDealers(latitude: Double, longitude: Double): Promise<List<Dealer>>

}
