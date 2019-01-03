package com.android.kubota

import android.app.Application
import com.kubota.repository.user.PCASetting
import com.microsoft.identity.client.PublicClientApplication

class MyKubotaApplication: Application() {

    lateinit var pca: PublicClientApplication

    override fun onCreate() {
        super.onCreate()

        val pcaSetting = PCASetting.SignIn()
        pca = PublicClientApplication(applicationContext, pcaSetting.clientId, pcaSetting.authority)
    }
}