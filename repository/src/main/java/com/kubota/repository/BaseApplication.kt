package com.kubota.repository

import android.app.Application
import android.content.Intent
import com.kubota.repository.service.PreferenceSyncService
import com.kubota.repository.utils.CacheUtils
import com.microsoft.identity.client.PublicClientApplication

abstract class BaseApplication: Application(), CacheUtils.CacheUtilsFactory {

    lateinit var pca: PublicClientApplication

    override fun onCreate() {
        super.onCreate()

        initCache(this)
        createPublicClientApplication()

        //TODO(JC): Fix Intent Filters so we do not have to specify class name
        val intent = Intent(this, PreferenceSyncService::class.java)
        intent.action = Intent.ACTION_SYNC
        startService(intent)
    }

    abstract fun createPublicClientApplication()
}