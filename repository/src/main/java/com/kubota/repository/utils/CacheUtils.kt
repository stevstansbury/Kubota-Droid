package com.kubota.repository.utils

import android.content.Context
import okhttp3.Cache

internal class CacheUtils private constructor(context: Context) {
    private val cache = Cache(context.cacheDir, 10 * 1024 * 1024)

    companion object {
        private var cacheUtils: CacheUtils? = null

        fun getCacheInstance(): Cache {
            return cacheUtils?.cache!!
        }
    }

    internal interface CacheUtilsFactory {
        fun initCache(context: Context) {
            cacheUtils = CacheUtils(context)
        }
    }
}