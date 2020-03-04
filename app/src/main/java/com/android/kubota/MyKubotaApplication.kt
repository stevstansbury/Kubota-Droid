package com.android.kubota

import com.kubota.repository.BaseApplication
import com.squareup.picasso.Picasso
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class MyKubotaApplication: BaseApplication() {

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())
        Picasso.with(this)
    }
}