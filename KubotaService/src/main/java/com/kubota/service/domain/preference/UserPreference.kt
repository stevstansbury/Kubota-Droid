//
//  UserPreference.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain.preference

import com.kubota.service.domain.Dealer
import com.kubota.service.domain.EquipmentUnit
import java.util.*

data class UserPreference(
    val userId: UUID,
    val equipment: List<EquipmentUnit>?,
    val dealers: List<Dealer>?
)
