package com.android.kubota

import com.kubota.repository.BaseApplication
import com.kubota.repository.user.PCASetting
import com.microsoft.identity.client.PublicClientApplication
import com.squareup.picasso.Picasso
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class MyKubotaApplication: BaseApplication() {

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())
        Picasso.with(this)
    }

    override fun createPublicClientApplication() {
        val pcaSetting = PCASetting.SignIn()
        pca = PublicClientApplication(applicationContext, pcaSetting.clientId, pcaSetting.authority)
    }
}