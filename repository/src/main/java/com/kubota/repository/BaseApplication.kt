package com.kubota.repository

import android.app.Application
import android.content.Intent
import com.kubota.network.service.CacheUtils
import com.kubota.repository.service.PreferenceSyncService
import com.microsoft.identity.client.PublicClientApplication

abstract class BaseApplication: Application() {

    lateinit var pca: PublicClientApplication

    override fun onCreate() {
        super.onCreate()

        val factory = object : CacheUtils.CacheUtilsFactory {}
        factory.initCache(this)

        createPublicClientApplication()

        //TODO(JC): Fix Intent Filters so we do not have to specify class name
        val intent = Intent(this, PreferenceSyncService::class.java)
        intent.action = Intent.ACTION_SYNC
        startService(intent)
    }

    abstract fun createPublicClientApplication()
}