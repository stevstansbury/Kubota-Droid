package com.kubota.network.service

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

internal object Utils {
    val HTTP_CLIENT: OkHttpClient by lazy {
        OkHttpClient.Builder().cache(CacheUtils.getCacheInstance()).build()
    }
    val MOSHI = Moshi.Builder().build()
}