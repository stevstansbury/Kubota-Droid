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
import com.inmotionsoftware.foundation.cache.MemDiskLruCacheStore
import com.inmotionsoftware.foundation.service.HTTPService
import com.kubota.service.BuildConfig
import com.kubota.service.api.*
import com.kubota.service.internal.KubotaAuthService
import com.kubota.service.internal.KubotaBrowseService
import com.kubota.service.internal.KubotaContentService
import com.kubota.service.internal.KubotaDealerService
import com.kubota.service.internal.KubotaGuidesService
import com.kubota.service.internal.KubotaUserPreferenceService
import com.kubota.service.internal.mock.MockKubotaEquipmentService
import com.kubota.service.internal.mock.MockKubotaNotificationService

@Throws
fun Database.clearUserDocuments() {
    this.inBatch {
        this.getDocument("UserPreferenceDocument")?.let { this.delete(it) }
        this.getDocument("UserSettingsDocument")?.let { this.delete(it) }
        this.getDocument("UserGeofencesDocument")?.let { this.delete(it) }
        this.getDocument("UserFavoriteDealersDocument")?.let { this.delete(it) }
        this.getDocument("UserEquipmentDocument")?.let { this.delete(it) }

        // Add more later
    }
}

class KubotaServiceManager(private val configuration: KubotaServiceConfiguration): ServiceManager {
    private val httpConfig = this.configuration.httpServiceConfig
    private var couchbaseDb: Database? = null
    private val contentHttpConfig: HTTPService.Config
        get() {
            val config = HTTPService.Config(baseUrl = null)
            this.configuration.context.get()?.let {
                val cacheDir = it.cacheDir
                val diskCacheSize = 100 * 1024 * 1024
                val memCacheSize = 2 * 1024 * 1024

                config.cacheStore = MemDiskLruCacheStore(cacheDir, diskCacheSize.toLong(), memCacheSize)
            }

            config.isAlwaysTrustHost = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "qa"
            config.enableHttpLogging = if (BuildConfig.DEBUG) this.configuration.enableHttpLogging else false
            config.requestTimeoutInterval = this.configuration.requestTimeoutInterval
            return config
        }

    init {
        this.configuration.context.get()?.let {
            CouchbaseLite.init(it)
            this.couchbaseDb = Database("MyKubota", DatabaseConfiguration())
        }
    }

    override val browseService: BrowseService
        get() = KubotaBrowseService(couchbaseDb = this.couchbaseDb)

    override val contentService: ContentService
        get() = KubotaContentService(config = this.contentHttpConfig)

    override val dealerService: DealerService
        get() = KubotaDealerService(config = this.httpConfig, couchbaseDb = this.couchbaseDb)

    override val equipmentService: EquipmentService
        get() = MockKubotaEquipmentService(config = this.httpConfig, couchbaseDb = this.couchbaseDb)

    override val guidesService: GuidesService
        get() = KubotaGuidesService()

    override val userPreferenceService: UserPreferenceService
        get() = KubotaUserPreferenceService(config = this.httpConfig,
                                            couchbaseDb = this.couchbaseDb,
                                            token = this.configuration.authToken)

    override val authService: AuthService
        get() =  KubotaAuthService(config = this.httpConfig,
                                   clientId = this.configuration.environment.clientId,
                                   clientSecret = this.configuration.environment.clientSecret,
                                   couchbaseDb = this.couchbaseDb)

    override val notificationService: NotificationService
        get() = MockKubotaNotificationService()
}
