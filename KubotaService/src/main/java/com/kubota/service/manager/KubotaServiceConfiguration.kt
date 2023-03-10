//
//  KubotaServiceConfiguration.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.kubota.service.manager

import android.content.Context
import com.inmotionsoftware.foundation.cache.MemDiskLruCacheStore
import com.inmotionsoftware.foundation.service.HTTPService
import com.inmotionsoftware.foundation.service.JSONDecoder
import com.inmotionsoftware.foundation.service.JSONEncoder
import com.inmotionsoftware.foundation.service.TimeInterval
import com.kubota.service.BuildConfig
import com.kubota.service.domain.ModelTypeJsonAdapter
import com.kubota.service.domain.auth.OAuthToken
import java.lang.ref.WeakReference
import java.net.URL

data class KubotaServiceEnvironment(
    val baseUrl: URL,
    val clientId: String,
    val clientSecret: String
)

data class KubotaServiceConfiguration(
    val context: WeakReference<Context>,
    val environment: KubotaServiceEnvironment,
    val authToken: OAuthToken? = null,
    val requestTimeoutInterval: TimeInterval = 120,
    val localeIdentifier: String,
    val enableHttpLogging: Boolean = BuildConfig.DEBUG
)

internal val KubotaServiceConfiguration.httpServiceConfig: HTTPService.Config
    get() {
        val httpServiceConfig = HTTPService.Config(
            baseUrl = this.environment.baseUrl,
            context = context.get(),
            decoders = mapOf(
                "application/json" to JSONDecoder(adapters = arrayOf(ModelTypeJsonAdapter()))
            ),
            encoders = mapOf(
                "application/json" to JSONEncoder(adapters = arrayOf(ModelTypeJsonAdapter()))
            )
        )
        val headers = mutableMapOf<String, String>()

        headers["version"] = "2022_R03"

        this.authToken?.let {
            headers["Authorization"] = "${it.tokenType} ${it.accessToken}"
        }
        headers["Accept-Language"] = this.localeIdentifier
        headers["Cache-Control"] = "no-cache"
        httpServiceConfig.headers = headers

        context.get()?.let {
            val cacheDir = it.cacheDir
            val diskCacheSize = 50 * 1024 * 1024
            val memCacheSize = 2 * 1024 * 1024

            httpServiceConfig.cacheStore = MemDiskLruCacheStore(cacheDir, diskCacheSize.toLong(), memCacheSize)
        }

        httpServiceConfig.isAlwaysTrustHost = BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "qa" || BuildConfig.BUILD_TYPE == "beta"
        httpServiceConfig.enableHttpLogging = if (BuildConfig.DEBUG) this.enableHttpLogging else false
        httpServiceConfig.requestTimeoutInterval = this.requestTimeoutInterval

        return httpServiceConfig
    }

