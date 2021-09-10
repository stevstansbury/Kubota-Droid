//
//  ServiceManager.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.api

interface ServiceManager {

    val browseService: BrowseService

    val contentService: ContentService

    val dealerService: DealerService

    val equipmentService: EquipmentService

    val faultService: FaultService

    val guidesService: GuidesService
    
    val userPreferenceService: UserPreferenceService

    val authService: AuthService

}
