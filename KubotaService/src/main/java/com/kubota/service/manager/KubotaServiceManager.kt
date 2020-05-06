//
//  KubotaServiceManager.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.manager

import com.kubota.service.api.*
import com.kubota.service.internal.KubotaAuthService
import com.kubota.service.internal.KubotaDealerService
import com.kubota.service.internal.KubotaEquipmentService
import com.kubota.service.internal.KubotaUserPreferenceService

class KubotaServiceManager(private val configuration: KubotaServiceConfiguration): ServiceManager {
    private val httpConfig = this.configuration.httpServiceConfig

    override val dealerService: DealerService
        get() = KubotaDealerService(config = this.httpConfig)

    override val equipmentService: EquipmentService
        get() = KubotaEquipmentService(config = this.httpConfig)

    override val userPreferenceService: UserPreferenceService
        get() = KubotaUserPreferenceService(config = this.httpConfig)

    override val authService: AuthService
        get() =  KubotaAuthService(config = this.httpConfig,
                                   clientId = this.configuration.environment.clientId,
                                   clientSecret = this.configuration.environment.clientSecret)
}
