package com.kubota.repository

import android.app.Application
import com.kubota.repository.utils.CacheUtils
import com.microsoft.identity.client.PublicClientApplication

abstract class BaseApplication: Application(), CacheUtils.CacheUtilsFactory {

    lateinit var pca: PublicClientApplication

    override fun onCreate() {
        super.onCreate()

        initCache(this)
        createPublicClientApplication()
    }

    abstract fun createPublicClientApplication()
}