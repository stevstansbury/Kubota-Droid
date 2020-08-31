//
//  AppProxy.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.android.kubota.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.android.kubota.BuildConfig
import com.android.kubota.app.account.AccountManager
import com.android.kubota.app.account.AccountManagerDelegate
import com.squareup.picasso.Picasso
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.PMKConfiguration
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.manager.KubotaServiceConfiguration
import com.kubota.service.manager.KubotaServiceEnvironment
import com.kubota.service.manager.KubotaServiceManager
import com.kubota.service.manager.SettingsRepoFactory
import io.fabric.sdk.android.Fabric
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executor

class AppProxy: Application(), AccountManagerDelegate {

    companion object {
        lateinit var proxy: AppProxy
    }

    private val environment: KubotaServiceEnvironment = KubotaServiceEnvironment(
        baseUrl = URL(BuildConfig.BASE_URL),
        clientId = BuildConfig.CLIENT_ID,
        clientSecret = BuildConfig.CLIENT_SECRET
    )

    lateinit var serviceManager: KubotaServiceManager
        private set

    lateinit var accountManager: AccountManager
        private set

    lateinit var preferences: AppPreferences
        private set

    var fcmToken: String? = null

    override fun onCreate() {
        super.onCreate()
        SettingsRepoFactory.getSettingsRepo(this)
        FirebaseApp.initializeApp(this)
        Fabric.with(this, Crashlytics())
        Picasso.with(this)

        this.initializeAppProxyInstance()
    }

    private fun initializeAppProxyInstance() {
        proxy = this

        // Setup main thread executor for PromiseKt
        val main = Executor { command -> Handler(Looper.getMainLooper()).post(command) }
        PMKConfiguration.Q = PMKConfiguration.Value(map = main, `return` = main)

        this.preferences = AppPreferences(context = this)
        this.accountManager = AccountManager(delegate = this)
        this.serviceManager =
            KubotaServiceManager(configuration =
                KubotaServiceConfiguration(context = WeakReference(this.applicationContext), environment = this.environment, authToken = this.accountManager.authToken))

        // Load user settings so we can
        if (accountManager.isAuthenticated.value == true) {
            accountManager.refreshUserSettings()
        }

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            this.fcmToken = it.token
        }
    }

    //
    // AccountManagerDelegate
    //

    override fun didAuthenticate(token: OAuthToken): Guarantee<Unit> {
        this.serviceManager = KubotaServiceManager(configuration =
            KubotaServiceConfiguration(context = WeakReference(this.applicationContext), environment = this.environment, authToken = token))
        return Guarantee.value(Unit)
    }

    override fun willUnauthenticate() {
    }

    override fun didUnauthenticate() {
        this.serviceManager = KubotaServiceManager(configuration =
            KubotaServiceConfiguration(context = WeakReference(this.applicationContext), environment = this.environment))
    }

}