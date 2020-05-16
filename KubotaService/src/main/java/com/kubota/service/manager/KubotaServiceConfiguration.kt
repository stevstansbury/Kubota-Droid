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
import com.inmotionsoftware.foundation.service.TimeInterval
import com.kubota.service.BuildConfig
import com.kubota.service.domain.auth.OAuthToken
import java.lang.ref.WeakReference
import java.net.URL

data class KubotaServiceEnvironment(val baseUrl: URL, val clientId: String, val clientSecret: String)

data class KubotaServiceConfiguration(
    val context: WeakReference<Context>,
    val environment: KubotaServiceEnvironment,
    val authToken: OAuthToken? = null,
    val requestTimeoutInterval: TimeInterval = 15,
    val enableHttpLogging: Boolean = BuildConfig.DEBUG
)

internal val KubotaServiceConfiguration.httpServiceConfig: HTTPService.Config
    get() {
        val httpServiceConfig = HTTPService.Config(baseUrl = this.environment.baseUrl)
        this.authToken?.let {
            httpServiceConfig.headers = mapOf(
                "Authorization" to "${it.tokenType} ${it.accessToken}"
            )
        }

        context.get()?.let {
            val cacheDir = it.cacheDir
            val diskCacheSize = 10 * 1024 * 1024
            val memCacheSize = 2 * 1024 * 1024

            httpServiceConfig.cacheStore = MemDiskLruCacheStore(cacheDir, diskCacheSize.toLong(), memCacheSize)
        }

        httpServiceConfig.isAlwaysTrustHost = BuildConfig.DEBUG
        httpServiceConfig.enableHttpLogging = if (BuildConfig.DEBUG) this.enableHttpLogging else false
        httpServiceConfig.requestTimeoutInterval = this.requestTimeoutInterval

        return httpServiceConfig
    }
