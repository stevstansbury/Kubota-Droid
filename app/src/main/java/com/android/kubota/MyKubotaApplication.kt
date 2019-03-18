package com.android.kubota

import com.kubota.repository.BaseApplication
import com.kubota.repository.user.PCASetting
import com.microsoft.identity.client.PublicClientApplication

class MyKubotaApplication: BaseApplication() {

    override fun createPublicClientApplication() {
        val pcaSetting = PCASetting.SignIn()
        pca = PublicClientApplication(applicationContext, pcaSetting.clientId, pcaSetting.authority)
    }
}