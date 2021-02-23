//
//  AppProxy.kt
//
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//  Copyright © 2020 Kubota Tractor Corporation. All rights reserved.
//

package com.android.kubota.app

import android.annotation.SuppressLint
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.os.ConfigurationCompat
import com.android.kubota.BuildConfig
import com.android.kubota.app.account.AccountManager
import com.android.kubota.app.account.AccountManagerDelegate
import com.squareup.picasso.Picasso
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import com.inmotionsoftware.promisekt.Guarantee
import com.inmotionsoftware.promisekt.PMKConfiguration
import com.inmotionsoftware.promisekt.cauterize
import com.kubota.service.domain.auth.OAuthToken
import com.kubota.service.manager.KubotaServiceConfiguration
import com.kubota.service.manager.KubotaServiceEnvironment
import com.kubota.service.manager.KubotaServiceManager
import com.kubota.service.manager.SettingsRepoFactory
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*
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

    private val currentLocale: Locale
        get() {
            val locales = ConfigurationCompat.getLocales(resources.configuration)
            return if (locales.isEmpty) Locale.getDefault() else locales[0]
        }

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
                KubotaServiceConfiguration(
                    context = WeakReference(this.applicationContext),
                    environment = this.environment,
                    authToken = this.accountManager.authToken,
                    localeIdentifier = this.currentLocale.identifier
                ))

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
            KubotaServiceConfiguration(
                context = WeakReference(this.applicationContext),
                environment = this.environment,
                authToken = token,
                localeIdentifier = this.currentLocale.identifier
            ))
        return Guarantee.value(Unit)
    }

    override fun willUnauthenticate() {
    }

    override fun didUnauthenticate() {
        this.serviceManager = KubotaServiceManager(configuration =
            KubotaServiceConfiguration(
                context = WeakReference(this.applicationContext),
                environment = this.environment,
                localeIdentifier = this.currentLocale.identifier
            ))
    }

    fun onLocaleChanged() {
        this.serviceManager = KubotaServiceManager(
            configuration = KubotaServiceConfiguration(
                context = WeakReference(this.applicationContext),
                environment = this.environment,
                authToken = this.accountManager.authToken,
                localeIdentifier = this.currentLocale.identifier
            )
        )

        preferences.setLanguageTag(Locale.getDefault().toLanguageTag())
        fcmToken?.let { token ->
            @SuppressLint("HardwareIds")
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            serviceManager.userPreferenceService.registerFCMToken(token, deviceId).cauterize()
        }
    }
}

///
/// Locale+Extension
///
val Locale.identifier: String
    get() {  return this.toLanguageTag() }
