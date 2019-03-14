package com.android.kubota

import android.app.Application
import com.kubota.repository.user.PCASetting
import com.kubota.repository.utils.CacheUtils
import com.microsoft.identity.client.PublicClientApplication

class MyKubotaApplication: Application(), CacheUtils.CacheUtilsFactory {

    lateinit var pca: PublicClientApplication

    override fun onCreate() {
        super.onCreate()

        initCache(this)
        val pcaSetting = PCASetting.SignIn()
        pca = PublicClientApplication(applicationContext, pcaSetting.clientId, pcaSetting.authority)
    }
}