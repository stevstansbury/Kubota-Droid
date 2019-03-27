package com.kubota.network.service

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

internal object Utils {
    val HTTP_CLIENT = OkHttpClient()
    val MOSHI = Moshi.Builder().build()
}