package com.kubota.repository.ext

import android.content.Context
import com.kubota.repository.BaseApplication

fun Context.getPublicClientApplication() = (applicationContext as BaseApplication).pca