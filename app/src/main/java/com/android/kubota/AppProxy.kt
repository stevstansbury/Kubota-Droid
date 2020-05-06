//
//  AppProxy.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.android.kubota

import android.accounts.AccountManager
import android.os.Handler
import android.os.Looper
import com.kubota.repository.BaseApplication
import com.squareup.picasso.Picasso
import com.crashlytics.android.Crashlytics
import com.inmotionsoftware.promisekt.PMKConfiguration
import com.kubota.service.manager.KubotaServiceConfiguration
import com.kubota.service.manager.KubotaServiceEnvironment
import com.kubota.service.manager.KubotaServiceManager
import io.fabric.sdk.android.Fabric
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.Executor

class AppProxy: BaseApplication() {

    companion object {
        lateinit var proxy: AppProxy
    }

    private lateinit var environment: KubotaServiceEnvironment
    lateinit var serviceManager: KubotaServiceManager
        private set

    lateinit var accountManager: AccountManager
        private set

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Picasso.with(this)

        this.initializeAppProxyInstance()
    }

    private fun initializeAppProxyInstance() {
        proxy = this

        // Setup main thread executor for PromiseKt
        val main = Executor { command -> Handler(Looper.getMainLooper()).post(command) }
        PMKConfiguration.Q = PMKConfiguration.Value(map = main, `return` = main)

        this.environment = KubotaServiceEnvironment(
            baseUrl = URL(BuildConfig.BASE_URL),
            clientId = BuildConfig.CLIENT_ID,
            clientSecret = BuildConfig.CLIENT_SECRET
        )

        this.serviceManager =
            KubotaServiceManager(configuration = KubotaServiceConfiguration(context = WeakReference(this.applicationContext), environment = this.environment))

//        this.accountManager = AccountManager(environment = this.environment, delegate = this)
//
//        if let authToken = self.accountManager.authToken {
//            self.protectedServiceManager =
//                KubotaProtectedServiceManager(configuration: KubotaServiceConfiguration.Protected(environment: self.environment, authToken: authToken))
//        }

    }

}