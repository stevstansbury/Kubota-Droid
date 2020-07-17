//
//  EquipmentCategory.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.domain

data class EquipmentCategory(
    val category: String,
    val parentCategory: String?,
    val hasSubCategories: Boolean,
    val imageResources: ImageResources?
)
