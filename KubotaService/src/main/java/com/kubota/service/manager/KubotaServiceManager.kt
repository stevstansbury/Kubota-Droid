//
//  KubotaServiceManager.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.manager

import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration
import com.kubota.service.api.*
import com.kubota.service.internal.KubotaAuthService
import com.kubota.service.internal.KubotaDealerService
import com.kubota.service.internal.KubotaEquipmentService
import com.kubota.service.internal.KubotaUserPreferenceService

class KubotaServiceManager(private val configuration: KubotaServiceConfiguration): ServiceManager {
    private val httpConfig = this.configuration.httpServiceConfig
    private var couchbaseDb: Database? = null

    init {
        this.configuration.context.get()?.let {
            CouchbaseLite.init(it)
            this.couchbaseDb = Database("MyKubota", DatabaseConfiguration())
        }
    }

    override val dealerService: DealerService
        get() = KubotaDealerService(config = this.httpConfig, couchbaseDb = this.couchbaseDb)

    override val equipmentService: EquipmentService
        get() = KubotaEquipmentService(config = this.httpConfig, couchbaseDb = this.couchbaseDb)

    override val userPreferenceService: UserPreferenceService
        get() = KubotaUserPreferenceService(config = this.httpConfig, couchbaseDb = this.couchbaseDb)

    override val authService: AuthService
        get() =  KubotaAuthService(config = this.httpConfig,
                                   clientId = this.configuration.environment.clientId,
                                   clientSecret = this.configuration.environment.clientSecret,
                                   couchbaseDb = this.couchbaseDb)
}
