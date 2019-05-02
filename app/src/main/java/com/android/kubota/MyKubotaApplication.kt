package com.android.kubota

import com.kubota.repository.BaseApplication
import com.kubota.repository.user.PCASetting
import com.microsoft.identity.client.PublicClientApplication
import com.squareup.picasso.Picasso

class MyKubotaApplication: BaseApplication() {

    override fun onCreate() {
        super.onCreate()

        Picasso.with(this)
    }

    override fun createPublicClientApplication() {
        val pcaSetting = PCASetting.SignIn()
        pca = PublicClientApplication(applicationContext, pcaSetting.clientId, pcaSetting.authority)
    }
}