//
//  KubotaServiceManager.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.manager

import com.couchbase.lite.*
import com.inmotionsoftware.foundation.cache.MemDiskLruCacheStore
import com.inmotionsoftware.foundation.service.HTTPService
import com.kubota.service.BuildConfig
import com.kubota.service.api.*
import com.kubota.service.internal.KubotaAuthService
import com.kubota.service.internal.KubotaBrowseService
import com.kubota.service.internal.KubotaContentService
import com.kubota.service.internal.KubotaDealerService
import com.kubota.service.internal.KubotaEquipmentService
import com.kubota.service.internal.KubotaGuidesService
import com.kubota.service.internal.KubotaUserPreferenceService
import com.kubota.service.internal.couchbase.DictionaryDecoder
import com.kubota.service.internal.couchbase.DictionaryEncoder

private data class PreferencesDocument(
    val localeIdentifier: String
)

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

@Throws
private fun Database.clearRecentViewedItems() {
    this.getDocument("RecentViewedItems")?.let { this.delete(it) }
}

@Throws
private fun Database.clearCategoriesAndModels() {
    val categoryQuery = QueryBuilder
        .select(SelectResult.property("category"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentCategory"))
        )

    for (result in categoryQuery.execute()) {
        result.toMap()
            .let { it["category"] as? Map<String, Any> }
            ?.let { it["category"] as? String }
            ?.let { this.delete(this.getDocument(it)) }
    }


    val modelQuery = QueryBuilder
        .select(SelectResult.property("model"))
        .from(DataSource.database(this))
        .where(
            Expression.property("type").equalTo(Expression.string("EquipmentModel"))
        )

    for (result in modelQuery.execute()) {
        result.toMap()
            .let { it["model"] as? Map<String, Any> }
            ?.let { it["model"] as? String }
            ?.let { this.delete(this.getDocument(it)) }
    }
}

@Throws
private fun Database.savePreferences(prefs: PreferencesDocument) {
    val data = DictionaryEncoder().encode(prefs) ?: return
    val document = MutableDocument("PreferencesDocument", data)
    this.save(document)
}

@Throws
private fun Database.getPreferences(): PreferencesDocument? {
    val document = this.getDocument("PreferencesDocument") ?: return null
    val data = document.toMap()
    return DictionaryDecoder().decode(type = PreferencesDocument::class.java, value = data)
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

            config.isAlwaysTrustHost = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "qa" || BuildConfig.BUILD_TYPE == "beta"
            config.enableHttpLogging = if (BuildConfig.DEBUG) this.configuration.enableHttpLogging else false
            config.requestTimeoutInterval = this.configuration.requestTimeoutInterval
            return config
        }

    init {
        this.configuration.context.get()?.let {
            CouchbaseLite.init(it)
            this.couchbaseDb = Database("MyKubota", DatabaseConfiguration())

            val prefs: PreferencesDocument = {
                when (val savedPrefs = this.couchbaseDb?.getPreferences()) {
                    null -> {
                        val newPrefs = PreferencesDocument(localeIdentifier = this.configuration.localeIdentifier)
                        this.couchbaseDb?.savePreferences(newPrefs)
                        newPrefs
                    }
                    else ->
                        savedPrefs
                }
            }()

            if (this.configuration.localeIdentifier != prefs.localeIdentifier) {
                this.couchbaseDb?.apply {
                    clearUserDocuments()
                    clearCategoriesAndModels()
                    clearRecentViewedItems()
                    savePreferences(PreferencesDocument(localeIdentifier = configuration.localeIdentifier))
                }
            }
        }
    }

    override val browseService: BrowseService
        get() = KubotaBrowseService(couchbaseDb = this.couchbaseDb)

    override val contentService: ContentService
        get() = KubotaContentService(config = this.contentHttpConfig)

    override val dealerService: DealerService
        get() = KubotaDealerService(
                    config = this.httpConfig,
                    couchbaseDb = this.couchbaseDb,
                    localeIdentifier = this.configuration.localeIdentifier
                )

    override val equipmentService: EquipmentService
        get() = KubotaEquipmentService(
                    config = this.httpConfig,
                    couchbaseDb = this.couchbaseDb,
                    localeIdentifier = this.configuration.localeIdentifier
                )

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

}
