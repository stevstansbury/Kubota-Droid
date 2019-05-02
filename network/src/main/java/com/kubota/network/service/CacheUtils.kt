package com.kubota.network.service

import android.content.Context
import okhttp3.Cache

class CacheUtils private constructor(context: Context) {
    private val cache = Cache(context.cacheDir, 10 * 1024 * 1024)

    companion object {
        private var cacheUtils: CacheUtils? = null

        fun getCacheInstance(): Cache {
            return cacheUtils?.cache!!
        }
    }

    interface CacheUtilsFactory {
        fun initCache(context: Context) {
            cacheUtils = CacheUtils(context)
        }
    }
}